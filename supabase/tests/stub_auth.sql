-- Supabase 의 auth 스키마를 흉내 낸 최소한의 껍데기.
--
-- 로컬에서 검사하려면 auth.users 와 auth.uid() 가 있어야 하고, 앱이 쓰는
-- 역할(anon · authenticated)도 있어야 한다. **권한까지 같게 맞추는 것이
-- 중요하다** — 권한이 없는 상태로 검사하면 RLS 가 아니라 권한 부족으로
-- 막히고, 그러면 정작 RLS 가 동작하는지는 확인되지 않는다.
--
-- 이 파일은 Supabase 에 올리지 않는다. 진짜 auth 스키마는 Supabase 가 만든다.

create extension if not exists pgcrypto;
create schema if not exists auth;

create table if not exists auth.users (
  id uuid primary key default gen_random_uuid(),
  email text,
  raw_user_meta_data jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

-- 로그인한 사람. 실제 Supabase 에서는 JWT 에서 온다.
create or replace function auth.uid() returns uuid
language sql stable as $$
  select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid
$$;

do $$ begin
  create role anon nologin;
exception when duplicate_object then null; end $$;

do $$ begin
  create role authenticated nologin;
exception when duplicate_object then null; end $$;

grant usage on schema public to anon, authenticated;
grant usage on schema auth to anon, authenticated;
grant select on auth.users to authenticated;

-- 실제 Supabase 와 같은 기본 권한. 새로 만드는 표에 자동으로 붙는다.
alter default privileges in schema public grant all on tables to anon, authenticated;
alter default privileges in schema public grant all on sequences to anon, authenticated;
