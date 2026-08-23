-- ============================================================
-- Minimuv — Çift İzleme Takip Uygulaması veritabanı şeması
-- ============================================================

-- ── Profiller (sabit iki satır) ──────────────────────────────
create table if not exists public.profiles (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  emoji text,
  avatar_color text,
  avatar_url text,
  created_at timestamptz not null default now()
);

-- Profil fotoğrafı bucket'ı (public)
insert into storage.buckets (id, name, public)
values ('avatars', 'avatars', true)
on conflict (id) do nothing;
create policy "avatars_public_read" on storage.objects
  for select to anon, authenticated using (bucket_id = 'avatars');
create policy "avatars_public_write" on storage.objects
  for insert to anon, authenticated with check (bucket_id = 'avatars');
create policy "avatars_public_update" on storage.objects
  for update to anon, authenticated using (bucket_id = 'avatars') with check (bucket_id = 'avatars');

insert into public.profiles (name, emoji, avatar_color)
select 'Van', '🌊', '#4EA8DE'
where not exists (select 1 from public.profiles where name = 'Van');

insert into public.profiles (name, emoji, avatar_color)
select 'Sinop', '🦜', '#FF8FA3'
where not exists (select 1 from public.profiles where name = 'Sinop');

-- ── Başlıklar ────────────────────────────────────────────────
create table if not exists public.titles (
  id uuid primary key default gen_random_uuid(),
  created_by_profile_id uuid references public.profiles(id) on delete set null,
  type text not null check (type in ('film', 'dizi', 'anime')),
  external_id text,
  title text not null,
  poster_url text,
  status text not null default 'Plan to Watch'
    check (status in ('Watching', 'Plan to Watch', 'Completed', 'Rewatching', 'Paused', 'Dropped')),
  score numeric check (score is null or (score >= 0 and score <= 10)),
  episode_progress int not null default 0 check (episode_progress >= 0),
  total_episodes int check (total_episodes is null or total_episodes > 0),
  start_date date,
  finish_date date,
  total_rewatches int not null default 0 check (total_rewatches >= 0),
  notes text,
  story numeric check (story is null or (story >= 0 and story <= 10)),
  characters numeric check (characters is null or (characters >= 0 and characters <= 10)),
  visuals numeric check (visuals is null or (visuals >= 0 and visuals <= 10)),
  audio numeric check (audio is null or (audio >= 0 and audio <= 10)),
  enjoyment numeric check (enjoyment is null or (enjoyment >= 0 and enjoyment <= 10)),
  custom_lists text[] not null default '{}',
  is_private boolean not null default false,
  watch_mode text not null default 'birlikte' check (watch_mode in ('birlikte', 'ayri')),
  priority_order int,
  is_favorite boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists titles_type_idx on public.titles (type);
create index if not exists titles_status_idx on public.titles (status);

create or replace function public.set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

drop trigger if exists titles_set_updated_at on public.titles;
create trigger titles_set_updated_at
  before update on public.titles
  for each row execute function public.set_updated_at();

-- ── Kişi bazlı bölüm ilerlemesi (ayrı mod) ───────────────────
create table if not exists public.episode_progress_per_profile (
  id uuid primary key default gen_random_uuid(),
  title_id uuid not null references public.titles(id) on delete cascade,
  profile_id uuid not null references public.profiles(id) on delete cascade,
  current_episode int not null default 0 check (current_episode >= 0),
  unique (title_id, profile_id)
);

-- ── Bölüm notları (spoiler korumalı) ─────────────────────────
create table if not exists public.episode_notes (
  id uuid primary key default gen_random_uuid(),
  title_id uuid not null references public.titles(id) on delete cascade,
  profile_id uuid not null references public.profiles(id) on delete cascade,
  episode_number int not null check (episode_number > 0),
  note_text text,
  emoji_reaction text,
  created_at timestamptz not null default now()
);

create index if not exists episode_notes_title_idx on public.episode_notes (title_id, episode_number);

-- ── Başarımlar (çift bazlı) ──────────────────────────────────
create table if not exists public.achievements (
  id uuid primary key default gen_random_uuid(),
  achievement_key text not null unique,
  unlocked_at timestamptz not null default now(),
  progress_current int,
  progress_target int
);

-- ── İzleme günlüğü (heatmap / istatistik) ────────────────────
create table if not exists public.watch_log (
  id uuid primary key default gen_random_uuid(),
  title_id uuid not null references public.titles(id) on delete cascade,
  profile_id uuid not null references public.profiles(id) on delete cascade,
  date date not null default current_date,
  episodes_watched int not null default 1 check (episodes_watched > 0)
);

create index if not exists watch_log_date_idx on public.watch_log (date);

-- ── Kişi bazlı puanlar (herkes kendi puanını verir) ──────────
create table if not exists public.title_scores (
  id uuid primary key default gen_random_uuid(),
  title_id uuid not null references public.titles(id) on delete cascade,
  profile_id uuid not null references public.profiles(id) on delete cascade,
  score numeric check (score is null or (score >= 0 and score <= 10)),
  story numeric check (story is null or (story >= 0 and story <= 10)),
  characters numeric check (characters is null or (characters >= 0 and characters <= 10)),
  visuals numeric check (visuals is null or (visuals >= 0 and visuals <= 10)),
  audio numeric check (audio is null or (audio >= 0 and audio <= 10)),
  enjoyment numeric check (enjoyment is null or (enjoyment >= 0 and enjoyment <= 10)),
  updated_at timestamptz not null default now(),
  unique (title_id, profile_id)
);

alter table public.title_scores enable row level security;
create policy anon_all on public.title_scores for all to anon, authenticated using (true) with check (true);
alter publication supabase_realtime add table public.title_scores;

-- ── Partnere gizli mesaj (bildirim) ──────────────────────────
create table if not exists public.partner_pings (
  id uuid primary key default gen_random_uuid(),
  from_profile uuid not null references public.profiles(id) on delete cascade,
  message text not null check (char_length(message) between 1 and 500),
  created_at timestamptz not null default now()
);
alter table public.partner_pings enable row level security;
create policy anon_all on public.partner_pings for all to anon, authenticated using (true) with check (true);
alter publication supabase_realtime add table public.partner_pings;

-- ── RLS: auth kullanılmıyor; anon anahtara tam erişim ────────
-- (Uygulama özel kullanım içindir, APK paylaşılmaz.)
alter table public.profiles enable row level security;
alter table public.titles enable row level security;
alter table public.episode_progress_per_profile enable row level security;
alter table public.episode_notes enable row level security;
alter table public.achievements enable row level security;
alter table public.watch_log enable row level security;

do $$
declare
  t text;
begin
  foreach t in array array[
    'profiles', 'titles', 'episode_progress_per_profile',
    'episode_notes', 'achievements', 'watch_log'
  ] loop
    execute format('drop policy if exists anon_all on public.%I', t);
    execute format(
      'create policy anon_all on public.%I for all to anon, authenticated using (true) with check (true)',
      t
    );
  end loop;
end $$;

-- ── Realtime ─────────────────────────────────────────────────
alter publication supabase_realtime add table public.titles;
alter publication supabase_realtime add table public.episode_progress_per_profile;
alter publication supabase_realtime add table public.episode_notes;
alter publication supabase_realtime add table public.achievements;
alter publication supabase_realtime add table public.watch_log;
