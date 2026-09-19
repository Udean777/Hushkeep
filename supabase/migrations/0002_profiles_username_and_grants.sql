alter table public.profiles
    add column if not exists username text;

alter table public.profiles
    drop constraint if exists profiles_username_check;

alter table public.profiles
    add constraint profiles_username_check
    check (username is null or username ~ '^[a-z0-9._-]{3,32}$');

create unique index if not exists profiles_username_lower_idx
    on public.profiles(lower(username));

grant usage on schema public to authenticated;

grant select, insert, update, delete on table
    public.profiles,
    public.albums,
    public.memories,
    public.media_objects
to authenticated;

grant all on table
    public.profiles,
    public.albums,
    public.memories,
    public.media_objects
to service_role;

revoke all on table
    public.profiles,
    public.albums,
    public.memories,
    public.media_objects
from anon;
