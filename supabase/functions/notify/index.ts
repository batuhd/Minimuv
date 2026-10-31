// Minimuv — FCM bildirim Edge Function'ı
// Veritabanı triggerları (pg_net) buraya POST atar; fonksiyon ilgili cihaz
// tokenlarına data-only FCM mesajı gönderir. Uygulama mesajı alınca Supabase'deki
// gerçek durumu kontrol edip bildirimi kendisi gösterir (çift bildirim korumalı).
//
// Güvenlik: Fonksiyon yalnızca doğru "x-webhook-secret" başlığını taşıyan
// istekleri kabul eder. Sır, public.app_config('webhook_secret') satırında
// tutulur; tablo RLS ile anon/authenticated erişimine kapalıdır ve yalnızca
// service_role (bu fonksiyon) ile security-definer trigger okuyabilir.
//
// Gereksinim: FCM_SERVICE_ACCOUNT secret'ı (Firebase service account JSON'u).
// Supabase Dashboard → Project Settings → Edge Functions → Secrets bölümünden
// "FCM_SERVICE_ACCOUNT" adıyla ekleyin (dosyanın TAM içeriği tek satır JSON).

import { SignJWT, importPKCS8 } from "npm:jose@5.9.6";
import { createClient } from "npm:@supabase/supabase-js@2.47.10";

const SA_RAW = Deno.env.get("FCM_SERVICE_ACCOUNT");
if (!SA_RAW) {
  throw new Error(
    "FCM_SERVICE_ACCOUNT secret eksik (Supabase Dashboard → Edge Functions → Secrets)",
  );
}

const SA = JSON.parse(SA_RAW);
const PROJECT_ID: string = SA.project_id;

// Token temizliği (geçersiz token silme) RLS'i atlamalı; service_role kullanılır.
const supabase = createClient(
  Deno.env.get("SUPABASE_URL")!,
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
);

// Webhook sırrı app_config'ten (service_role) okunur; soğuk başlangıçta bir kez.
let cachedWebhookSecret: string | null = null;
async function webhookSecret(): Promise<string> {
  if (cachedWebhookSecret !== null) return cachedWebhookSecret;
  const { data } = await supabase
    .from("app_config")
    .select("value")
    .eq("key", "webhook_secret")
    .maybeSingle();
  cachedWebhookSecret = data?.value ?? "";
  return cachedWebhookSecret;
}

let cached: { token: string; exp: number } | null = null;

async function accessToken(): Promise<string> {
  // exp milisaniye cinsinden tutulur; Date.now() ile aynı birimde karşılaştırılır.
  if (cached && cached.exp > Date.now() + 60_000) return cached.token;
  const now = Math.floor(Date.now() / 1000);
  const key = await importPKCS8(SA.private_key, "RS256");
  const jwt = await new SignJWT({
    scope: "https://www.googleapis.com/auth/firebase.messaging",
  })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(SA.client_email)
    .setSubject(SA.client_email)
    .setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(key);

  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });
  if (!res.ok) {
    throw new Error(`OAuth başarısız: ${res.status} ${await res.text()}`);
  }
  const data = await res.json();
  cached = {
    token: data.access_token,
    exp: Date.now() + (data.expires_in ?? 3600) * 1000,
  };
  return cached.token;
}

async function send(token: string, data: Record<string, string>) {
  const at = await accessToken();
  const res = await fetch(
    `https://fcm.googleapis.com/v1/projects/${PROJECT_ID}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${at}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: { token, data, android: { priority: "high" } },
      }),
    },
  );
  if (!res.ok) {
    const err = await res.json().catch(() => null);
    const code = err?.error?.details?.[0]?.errorCode;
    if (res.status === 404 || code === "UNREGISTERED" || code === "INVALID_ARGUMENT") {
      // Geçersiz token (uygulama silinmiş/yeniden kurulmuş) — temizle
      await supabase.from("fcm_tokens").delete().eq("token", token);
      console.log(`geçersiz token silindi (${code ?? res.status})`);
    } else {
      console.warn(`FCM ${res.status}: ${JSON.stringify(err)}`);
    }
  }
}

/** Bir token'ın hatası diğer token'ların gönderimini iptal etmez. */
async function sendAll(
  tokens: Array<{ token: string }> | null,
  data: Record<string, string>,
) {
  const results = await Promise.allSettled(
    (tokens ?? []).map((t) => send(t.token, data)),
  );
  const failed = results.filter((r) => r.status === "rejected").length;
  if (failed > 0) console.warn(`${failed} token gönderimi başarısız`);
}

const ALLOWED_TABLES = new Set([
  "partner_pings",
  "titles",
  "episode_progress_per_profile",
  "title_scores",
  "title_notes",
  "episode_notes",
]);
const ALLOWED_TYPES = new Set(["INSERT", "UPDATE"]);
const MAX_BODY_BYTES = 256_000;

function isPlainObject(value: unknown): value is Record<string, any> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("POST bekleniyor", { status: 405 });
  }
  const expectedSecret = await webhookSecret();
  if (!expectedSecret || req.headers.get("x-webhook-secret") !== expectedSecret) {
    return new Response("yetkisiz", { status: 401 });
  }

  const raw = await req.text();
  if (raw.length > MAX_BODY_BYTES) {
    return new Response("gövde çok büyük", { status: 413 });
  }
  let body: unknown;
  try {
    body = JSON.parse(raw);
  } catch {
    return new Response("geçersiz gövde", { status: 400 });
  }
  if (!isPlainObject(body)) {
    return new Response("geçersiz gövde", { status: 400 });
  }

  const { type, table, payload, old } = body as Record<string, any>;
  // Şema doğrulaması: yalnızca bilinen tablo/tür ve nesne payload kabul edilir.
  if (
    typeof table !== "string" ||
    !ALLOWED_TABLES.has(table) ||
    typeof type !== "string" ||
    !ALLOWED_TYPES.has(type) ||
    !isPlainObject(payload) ||
    (old !== null && old !== undefined && !isPlainObject(old))
  ) {
    return new Response("geçersiz şema", { status: 400 });
  }

  try {
    if (table === "partner_pings" && type === "INSERT" && payload?.from_profile) {
      const { data: tokens } = await supabase
        .from("fcm_tokens")
        .select("token, profile_id")
        .neq("profile_id", payload.from_profile);
      await sendAll(tokens, { type: "ping" });
    }

    if (table === "titles" && type === "INSERT") {
      // Ekleyen cihaza bildirim gönderme; eski kayıtlarda created_by null olabilir.
      const creator: string | undefined = payload?.created_by_profile_id;
      const { data: tokens } = creator
        ? await supabase.from("fcm_tokens").select("token, profile_id").neq("profile_id", creator)
        : await supabase.from("fcm_tokens").select("token, profile_id");
      await sendAll(tokens, { type: "title_new" });
    }

    if (table === "titles" && type === "UPDATE") {
      if (payload?.status && old?.status && payload.status !== old.status) {
        // Durumu değiştiren cihaza bildirim gönderme (updated_by_profile_id).
        const sender: string | undefined = payload?.updated_by_profile_id;
        const { data: tokens } = sender
          ? await supabase.from("fcm_tokens").select("token").neq("profile_id", sender)
          : await supabase.from("fcm_tokens").select("token");
        await sendAll(tokens, { type: "title_status" });
      }
    }

    if (table === "episode_progress_per_profile") {
      const { data: tokens } = await supabase
        .from("fcm_tokens")
        .select("token");
      await sendAll(tokens, { type: "episode_progress" });
    }

    if (table === "title_scores" && payload?.profile_id) {
      const { data: tokens } = await supabase
        .from("fcm_tokens")
        .select("token, profile_id")
        .neq("profile_id", payload.profile_id);
      await sendAll(tokens, { type: "score" });
    }

    if (table === "title_notes" && payload?.profile_id) {
      const { data: tokens } = await supabase
        .from("fcm_tokens")
        .select("token, profile_id")
        .neq("profile_id", payload.profile_id);
      await sendAll(tokens, { type: "note" });
    }

    if (table === "episode_notes" && payload?.profile_id) {
      const { data: tokens } = await supabase
        .from("fcm_tokens")
        .select("token, profile_id")
        .neq("profile_id", payload.profile_id);
      await sendAll(tokens, { type: "episode_note" });
    }
  } catch (e) {
    console.error(e);
    return new Response(JSON.stringify({ ok: false, error: String(e) }), {
      status: 500,
    });
  }

  return new Response(JSON.stringify({ ok: true }));
});
