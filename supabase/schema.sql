-- PolyInsights MVP schema (authenticated users + Google OAuth via Supabase Auth)
-- Run this entire script in Supabase SQL Editor.

-- Recommended extension for UUID helpers.
create extension if not exists pgcrypto;

-- Keep app tables in public schema.

-- Generic timestamp trigger function.
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = timezone('utc', now());
  return new;
end;
$$;

-- User profile table (captures email + basic metadata from auth.users)
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text not null unique,
  full_name text,
  avatar_url text,
  auth_provider text,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

-- App query sessions
create table if not exists public.queries (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  question text not null,
  sophistication_score integer,
  sophistication_label text check (sophistication_label in ('low', 'high')),
  status text not null default 'analysis' check (status in ('analysis', 'review', 'reporting', 'completed', 'failed')),
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

-- Cached market records fetched for a query
create table if not exists public.market_data (
  id uuid primary key default gen_random_uuid(),
  query_id uuid not null references public.queries(id) on delete cascade,
  market_id text,
  market_title text not null,
  volume_numeric numeric(20, 4),
  yes_probability numeric(7, 6),
  no_probability numeric(7, 6),
  sentiment text check (sentiment in ('bullish', 'bearish', 'neutral', 'mixed')),
  source text not null default 'polymarket',
  raw_payload jsonb,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

-- Intermediate summary (200-300 words)
create table if not exists public.summaries (
  id uuid primary key default gen_random_uuid(),
  query_id uuid not null unique references public.queries(id) on delete cascade,
  summary_text text not null,
  model_name text,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

-- Final long-form report
create table if not exists public.reports (
  id uuid primary key default gen_random_uuid(),
  query_id uuid not null unique references public.queries(id) on delete cascade,
  report_format text not null default 'markdown' check (report_format in ('markdown', 'html', 'json')),
  report_content text,
  report_json jsonb,
  status text not null default 'pending' check (status in ('pending', 'completed', 'failed')),
  error_message text,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

-- Helpful indexes
create index if not exists idx_queries_user_id_created_at on public.queries(user_id, created_at desc);
create index if not exists idx_market_data_query_id on public.market_data(query_id);
create index if not exists idx_market_data_market_id on public.market_data(market_id);
create index if not exists idx_reports_status on public.reports(status);

-- Auto update updated_at
drop trigger if exists trg_profiles_set_updated_at on public.profiles;
create trigger trg_profiles_set_updated_at
before update on public.profiles
for each row execute function public.set_updated_at();

drop trigger if exists trg_queries_set_updated_at on public.queries;
create trigger trg_queries_set_updated_at
before update on public.queries
for each row execute function public.set_updated_at();

drop trigger if exists trg_market_data_set_updated_at on public.market_data;
create trigger trg_market_data_set_updated_at
before update on public.market_data
for each row execute function public.set_updated_at();

drop trigger if exists trg_summaries_set_updated_at on public.summaries;
create trigger trg_summaries_set_updated_at
before update on public.summaries
for each row execute function public.set_updated_at();

drop trigger if exists trg_reports_set_updated_at on public.reports;
create trigger trg_reports_set_updated_at
before update on public.reports
for each row execute function public.set_updated_at();

-- Create a profile row automatically when a new auth user is created
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  provider_name text;
begin
  provider_name := coalesce(new.raw_app_meta_data ->> 'provider', 'email');

  insert into public.profiles (id, email, full_name, avatar_url, auth_provider)
  values (
    new.id,
    coalesce(new.email, ''),
    coalesce(new.raw_user_meta_data ->> 'full_name', new.raw_user_meta_data ->> 'name'),
    new.raw_user_meta_data ->> 'avatar_url',
    provider_name
  )
  on conflict (id) do update set
    email = excluded.email,
    full_name = excluded.full_name,
    avatar_url = excluded.avatar_url,
    auth_provider = excluded.auth_provider,
    updated_at = timezone('utc', now());

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

-- Enable RLS
alter table public.profiles enable row level security;
alter table public.queries enable row level security;
alter table public.market_data enable row level security;
alter table public.summaries enable row level security;
alter table public.reports enable row level security;

-- Profiles policies (self access)
drop policy if exists "profiles_select_own" on public.profiles;
create policy "profiles_select_own"
on public.profiles
for select
to authenticated
using (id = auth.uid());

drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own"
on public.profiles
for update
to authenticated
using (id = auth.uid())
with check (id = auth.uid());

-- Queries policies (owner access)
drop policy if exists "queries_select_own" on public.queries;
create policy "queries_select_own"
on public.queries
for select
to authenticated
using (user_id = auth.uid());

drop policy if exists "queries_insert_own" on public.queries;
create policy "queries_insert_own"
on public.queries
for insert
to authenticated
with check (user_id = auth.uid());

drop policy if exists "queries_update_own" on public.queries;
create policy "queries_update_own"
on public.queries
for update
to authenticated
using (user_id = auth.uid())
with check (user_id = auth.uid());

drop policy if exists "queries_delete_own" on public.queries;
create policy "queries_delete_own"
on public.queries
for delete
to authenticated
using (user_id = auth.uid());

-- market_data policies (by parent query ownership)
drop policy if exists "market_data_select_own" on public.market_data;
create policy "market_data_select_own"
on public.market_data
for select
to authenticated
using (
  exists (
    select 1
    from public.queries q
    where q.id = market_data.query_id
      and q.user_id = auth.uid()
  )
);

drop policy if exists "market_data_insert_own" on public.market_data;
create policy "market_data_insert_own"
on public.market_data
for insert
to authenticated
with check (
  exists (
    select 1
    from public.queries q
    where q.id = market_data.query_id
      and q.user_id = auth.uid()
  )
);

drop policy if exists "market_data_update_own" on public.market_data;
create policy "market_data_update_own"
on public.market_data
for update
to authenticated
using (
  exists (
    select 1
    from public.queries q
    where q.id = market_data.query_id
      and q.user_id = auth.uid()
  )
)
with check (
  exists (
    select 1
    from public.queries q
    where q.id = market_data.query_id
      and q.user_id = auth.uid()
  )
);

drop policy if exists "market_data_delete_own" on public.market_data;
create policy "market_data_delete_own"
on public.market_data
for delete
to authenticated
using (
  exists (
    select 1
    from public.queries q
    where q.id = market_data.query_id
      and q.user_id = auth.uid()
  )
);

-- summaries policies (by parent query ownership)
drop policy if exists "summaries_select_own" on public.summaries;
create policy "summaries_select_own"
on public.summaries
for select
to authenticated
using (
  exists (
    select 1
    from public.queries q
    where q.id = summaries.query_id
      and q.user_id = auth.uid()
  )
);

drop policy if exists "summaries_insert_own" on public.summaries;
create policy "summaries_insert_own"
on public.summaries
for insert
to authenticated
with check (
  exists (
    select 1
    from public.queries q
    where q.id = summaries.query_id
      and q.user_id = auth.uid()
  )
);

drop policy if exists "summaries_update_own" on public.summaries;
create policy "summaries_update_own"
on public.summaries
for update
to authenticated
using (
  exists (
    select 1
    from public.queries q
    where q.id = summaries.query_id
      and q.user_id = auth.uid()
  )
)
with check (
  exists (
    select 1
    from public.queries q
    where q.id = summaries.query_id
      and q.user_id = auth.uid()
  )
);

drop policy if exists "summaries_delete_own" on public.summaries;
create policy "summaries_delete_own"
on public.summaries
for delete
to authenticated
using (
  exists (
    select 1
    from public.queries q
    where q.id = summaries.query_id
      and q.user_id = auth.uid()
  )
);

-- reports policies (by parent query ownership)
drop policy if exists "reports_select_own" on public.reports;
create policy "reports_select_own"
on public.reports
for select
to authenticated
using (
  exists (
    select 1
    from public.queries q
    where q.id = reports.query_id
      and q.user_id = auth.uid()
  )
);

drop policy if exists "reports_insert_own" on public.reports;
create policy "reports_insert_own"
on public.reports
for insert
to authenticated
with check (
  exists (
    select 1
    from public.queries q
    where q.id = reports.query_id
      and q.user_id = auth.uid()
  )
);

drop policy if exists "reports_update_own" on public.reports;
create policy "reports_update_own"
on public.reports
for update
to authenticated
using (
  exists (
    select 1
    from public.queries q
    where q.id = reports.query_id
      and q.user_id = auth.uid()
  )
)
with check (
  exists (
    select 1
    from public.queries q
    where q.id = reports.query_id
      and q.user_id = auth.uid()
  )
);

drop policy if exists "reports_delete_own" on public.reports;
create policy "reports_delete_own"
on public.reports
for delete
to authenticated
using (
  exists (
    select 1
    from public.queries q
    where q.id = reports.query_id
      and q.user_id = auth.uid()
  )
);

-- NOTE: Google OAuth setup is completed in Supabase Dashboard:
-- Auth -> Providers -> Google -> enable and add client credentials.
