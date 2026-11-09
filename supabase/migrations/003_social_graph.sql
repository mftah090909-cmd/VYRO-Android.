-- VYRO real social graph and direct messages
create table if not exists public.follows (
  follower_id uuid not null references public.profiles(id) on delete cascade,
  following_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (follower_id, following_id),
  check (follower_id <> following_id)
);
create table if not exists public.conversations (
  id uuid primary key default uuid_generate_v4(),
  created_at timestamptz not null default now()
);
create table if not exists public.conversation_members (
  conversation_id uuid not null references public.conversations(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  joined_at timestamptz not null default now(),
  primary key (conversation_id, user_id)
);
create table if not exists public.messages (
  id uuid primary key default uuid_generate_v4(),
  conversation_id uuid not null references public.conversations(id) on delete cascade,
  sender_id uuid not null references public.profiles(id) on delete cascade,
  body text not null check (length(body) between 1 and 4000),
  created_at timestamptz not null default now(),
  read_at timestamptz
);
create index if not exists follows_following_idx on public.follows(following_id);
create index if not exists messages_conversation_idx on public.messages(conversation_id, created_at);
alter table public.follows enable row level security;
alter table public.conversations enable row level security;
alter table public.conversation_members enable row level security;
alter table public.messages enable row level security;
create policy "follows readable" on public.follows for select using (true);
create policy "own follow insert" on public.follows for insert with check (auth.uid() = follower_id);
create policy "own follow delete" on public.follows for delete using (auth.uid() = follower_id);
create policy "conversation members readable" on public.conversation_members for select using (auth.uid() = user_id);
create policy "conversation member insert" on public.conversation_members for insert with check (auth.uid() = user_id);
create policy "messages readable by members" on public.messages for select using (exists (select 1 from public.conversation_members m where m.conversation_id = messages.conversation_id and m.user_id = auth.uid()));
create policy "members send messages" on public.messages for insert with check (auth.uid() = sender_id and exists (select 1 from public.conversation_members m where m.conversation_id = messages.conversation_id and m.user_id = auth.uid()));
create policy "sender updates messages" on public.messages for update using (auth.uid() = sender_id);
