-- Hushkeep Realtime publication
-- Apply this migration to the target Supabase project before testing
-- cross-device updates. It is intentionally safe to run more than once.

alter table public.albums replica identity full;
alter table public.memories replica identity full;
alter table public.media_objects replica identity full;

do $$
begin
    if exists (
        select 1
        from pg_publication
        where pubname = 'supabase_realtime'
    ) then
        if not exists (
            select 1
            from pg_publication_tables
            where pubname = 'supabase_realtime'
              and schemaname = 'public'
              and tablename = 'albums'
        ) then
            execute 'alter publication supabase_realtime add table public.albums';
        end if;

        if not exists (
            select 1
            from pg_publication_tables
            where pubname = 'supabase_realtime'
              and schemaname = 'public'
              and tablename = 'memories'
        ) then
            execute 'alter publication supabase_realtime add table public.memories';
        end if;

        if not exists (
            select 1
            from pg_publication_tables
            where pubname = 'supabase_realtime'
              and schemaname = 'public'
              and tablename = 'media_objects'
        ) then
            execute 'alter publication supabase_realtime add table public.media_objects';
        end if;
    end if;
end
$$;
