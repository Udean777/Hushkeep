create extension if not exists pgcrypto;

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    email text,
    username text check (username is null or username ~ '^[a-z0-9._-]{3,32}$'),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.albums (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null references auth.users(id) on delete cascade,
    name text not null check (char_length(trim(name)) between 1 and 120),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz
);

create table if not exists public.memories (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null references auth.users(id) on delete cascade,
    album_id uuid references public.albums(id) on delete set null,
    caption text check (caption is null or char_length(caption) <= 280),
    captured_at timestamptz not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    is_favorite boolean not null default false,
    deleted_at timestamptz
);

create table if not exists public.media_objects (
    id uuid primary key default gen_random_uuid(),
    memory_id uuid not null references public.memories(id) on delete cascade,
    owner_id uuid not null references auth.users(id) on delete cascade,
    file_name text not null,
    mime_type text not null,
    size_bytes bigint not null check (size_bytes >= 0),
    width integer,
    height integer,
    storage_path text not null unique,
    encryption_version integer not null default 0,
    key_id text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists albums_owner_id_idx on public.albums(owner_id);
create index if not exists memories_owner_captured_idx on public.memories(owner_id, captured_at desc);
create index if not exists memories_owner_album_idx on public.memories(owner_id, album_id);
create index if not exists media_objects_owner_id_idx on public.media_objects(owner_id);
create unique index if not exists profiles_username_lower_idx on public.profiles(lower(username));

alter table public.profiles enable row level security;
alter table public.albums enable row level security;
alter table public.memories enable row level security;
alter table public.media_objects enable row level security;

create policy "profiles are private to their owner"
on public.profiles for all
to authenticated
using (id = auth.uid())
with check (id = auth.uid());

create policy "albums are private to their owner"
on public.albums for all
to authenticated
using (owner_id = auth.uid())
with check (owner_id = auth.uid());

create policy "memories are private to their owner"
on public.memories for all
to authenticated
using (owner_id = auth.uid())
with check (owner_id = auth.uid());

create policy "media metadata is private to their owner"
on public.media_objects for all
to authenticated
using (owner_id = auth.uid())
with check (owner_id = auth.uid());

insert into storage.buckets (id, name, public)
values ('hushkeep-private', 'hushkeep-private', false)
on conflict (id) do update set public = false;

create policy "private media can be read by its owner"
on storage.objects for select
to authenticated
using (
    bucket_id = 'hushkeep-private'
    and (storage.foldername(name))[1] = auth.uid()::text
);

create policy "private media can be uploaded by its owner"
on storage.objects for insert
to authenticated
with check (
    bucket_id = 'hushkeep-private'
    and (storage.foldername(name))[1] = auth.uid()::text
);

create policy "private media can be updated by its owner"
on storage.objects for update
to authenticated
using (
    bucket_id = 'hushkeep-private'
    and (storage.foldername(name))[1] = auth.uid()::text
)
with check (
    bucket_id = 'hushkeep-private'
    and (storage.foldername(name))[1] = auth.uid()::text
);

create policy "private media can be deleted by its owner"
on storage.objects for delete
to authenticated
using (
    bucket_id = 'hushkeep-private'
    and (storage.foldername(name))[1] = auth.uid()::text
);
