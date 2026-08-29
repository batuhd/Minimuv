-- Başlık başına tek tek notlar (eskiden titles.notes tek metindi)
create table if not exists public.title_notes (
  id uuid primary key default gen_random_uuid(),
  title_id uuid not null references public.titles(id) on delete cascade,
  profile_id uuid not null references public.profiles(id) on delete cascade,
  note_text text not null check (char_length(note_text) between 1 and 2000),
  created_at timestamptz not null default now()
);
create index if not exists title_notes_title_idx on public.title_notes (title_id);
alter table public.title_notes enable row level security;
create policy anon_all_title_notes on public.title_notes for all to anon using (true) with check (true);
alter publication supabase_realtime add table public.title_notes;

-- Mevcut tek metinli notları tekil notlara taşı (kopya; titles.notes'e dokunulmaz)
insert into public.title_notes (title_id, profile_id, note_text)
select t.id, coalesce(t.created_by_profile_id, (select id from public.profiles order by name limit 1)), t.notes
from public.titles t
where t.notes is not null and char_length(trim(t.notes)) > 0
on conflict do nothing;

-- Sıfırlama fonksiyonunu yeni tabloyu da kapsayacak şekilde güncelle
create or replace function public.reset_all_data()
returns void
language sql
security definer
set search_path = public
as $$
  truncate table public.title_notes,
    public.partner_pings,
    public.title_scores,
    public.episode_notes,
    public.episode_progress_per_profile,
    public.watch_log,
    public.achievements,
    public.titles;
$$;
grant execute on function public.reset_all_data() to anon, authenticated;
