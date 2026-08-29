-- Başlık detay sayfasında gösterilmek üzere API'den gelen açıklama (özet)
alter table public.titles add column if not exists overview text;
