-- VYRO Rooms: metadata and membership for real LiveKit audio rooms
create table if not exists public.rooms (
  id uuid primary key default uuid_generate_v4(),
  room_name text not null unique check (length(room_name) between 3 and 80),
  title text not null check (length(title) between 1 and 120),
  host_id uuid not null references public.profiles(id) on delete cascade,
  status text not null default 'live' check (status in ('live','ended')),
  created_at timestamptz not null default now(),
  ended_at timestamptz
);
create table if not exists public.room_members (
  room_id uuid not null references public.rooms(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  role text not null default 'listener' check (role in ('host','moderator','speaker','listener')),
  joined_at timestamptz not null default now(),
  primary key (room_id, user_id)
);
create index if not exists rooms_live_idx on public.rooms(status, created_at desc);
alter table public.rooms enable row level security;
alter table public.room_members enable row level security;
create policy "live rooms readable" on public.rooms for select using (status = 'live' or auth.uid() = host_id);
create policy "own rooms insert" on public.rooms for insert with check (auth.uid() = host_id);
create policy "host updates rooms" on public.rooms for update using (auth.uid() = host_id);
create policy "members readable" on public.room_members for select using (true);
create policy "users join rooms" on public.room_members for insert with check (auth.uid() = user_id);
create policy "users leave rooms" on public.room_members for delete using (auth.uid() = user_id);
