-- ============================================================
-- Minimuv — Denetim güvenlik sertleştirmesi (2026-10-30)
--
-- ÖNEMLİ: Bu dosya BU DENETİMDE CANLI VERİTABANINA UYGULANMADI.
-- Yalnızca supabase/init.sql ile eşleşen, idempotent bir dağıtım
-- dosyasıdır. Uygulamadan önce supabase/init.sql de güncellenmiş
-- olmalıdır (tek kaynak init.sql'dir).
--
-- Uygulama sırası (insan tarafından, Supabase SQL Editor):
--   1) Edge Function secret'ı: WEBHOOK_SECRET = <rastgele güçlü sır>
--   2) alter database postgres set app.settings.notify_url = 'https://<proje>.supabase.co/functions/v1/notify';
--      alter database postgres set app.settings.notify_secret = '<aynı sır>';
--      (veya Supabase Vault kullanın; değer repoya yazılmaz)
--   3) Bu dosyayı çalıştırın.
--
-- Kapsam:
--   SEC-001  reset_all_data() yalnızca service_role'de
--   SEC-008  fcm_notify_event: URL/sır ayarlardan, gömülü değer yok
--   QUAL-033 pg_net şeması istemci rollerine kapalı
--   DS-SEC-08 avatars: yol deseni + 2 MB + image/jpeg sınırı
--
-- Kapsam DIŞI (HUMAN_DECISIONS.md): anon_all RLS politikalarının
-- kaldırılması Supabase Auth/çift üyeliği tasarımı gerektirir.
-- ============================================================

-- ── SEC-001: yıkıcı RPC yalnızca service_role ────────────────
revoke all on function public.reset_all_data() from public, anon, authenticated;
grant execute on function public.reset_all_data() to service_role;

-- ── DS-SEC-08: avatars bucket sınırları ──────────────────────
-- Uygulama "<profil-uuid>.jpg" yoluna yükler (ProfileRepository.uploadAvatar).
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('avatars', 'avatars', true, 2097152, array['image/jpeg'])
on conflict (id) do update
  set file_size_limit = excluded.file_size_limit,
      allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "avatars_public_write" on storage.objects;
drop policy if exists "avatars_public_update" on storage.objects;
create policy "avatars_public_write" on storage.objects
  for insert to anon, authenticated
  with check (
    bucket_id = 'avatars'
    and name ~ '^[0-9a-fA-F-]{36}\.jpg$'
  );
create policy "avatars_public_update" on storage.objects
  for update to anon, authenticated
  using (bucket_id = 'avatars' and name ~ '^[0-9a-fA-F-]{36}\.jpg$')
  with check (bucket_id = 'avatars' and name ~ '^[0-9a-fA-F-]{36}\.jpg$');

-- ── QUAL-033: pg_net şeması istemci rollerine kapalı ─────────
grant usage on schema net to postgres, service_role;
revoke usage on schema net from anon, authenticated;

-- ── SEC-008: FCM trigger ayarlardan okur, gömülü sır yok ─────
create or replace function public.fcm_notify_event()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_url text := nullif(current_setting('app.settings.notify_url', true), '');
  v_secret text := nullif(current_setting('app.settings.notify_secret', true), '');
begin
  if v_url is null or v_secret is null then
    raise warning 'fcm_notify_event: app.settings.notify_url / notify_secret tanimli degil; bildirim atlandi';
    return new;
  end if;

  perform net.http_post(
    url := v_url,
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'x-webhook-secret', v_secret
    ),
    body := jsonb_build_object(
      'type', tg_op,
      'table', tg_table_name,
      'payload', to_jsonb(new),
      'old', case when tg_op = 'UPDATE' then to_jsonb(old) else null end
    )
  );
  return new;
end;
$$;
