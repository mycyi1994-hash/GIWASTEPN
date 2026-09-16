-- 계정과 프로필.
--
-- Supabase 의 auth.users 는 건드리지 않는다. 그 표는 인증이 소유하고, 우리는
-- 곁에 프로필 표를 두어 1:1로 붙인다. 이렇게 두면 인증 방식이 바뀌어도
-- (구글에서 애플로, 또는 익명 계정 추가) 프로필은 그대로 산다.

create table if not exists public.profiles (
  -- auth.users 와 같은 id 를 쓴다. 계정이 지워지면 프로필도 함께 지워진다.
  id uuid primary key references auth.users on delete cascade,

  -- 화면에 보이는 이름. 게시글 작성자로 쓰인다.
  display_name text not null default '러너',
  avatar_id int not null default 0,

  -- 앱 설정 — 기기를 바꿔도 따라오게 서버에 둔다
  daily_goal int not null default 8000 check (daily_goal between 1000 and 100000),
  language text not null default '',

  -- 연속 달성 기록
  streak int not null default 0 check (streak >= 0),
  last_goal_met_day bigint not null default -1,

  -- 랭킹 재료. 세션이 쌓일 때 서버가 갱신한다.
  top_speed_kmh double precision not null default 0 check (top_speed_kmh >= 0),
  lifetime_km double precision not null default 0 check (lifetime_km >= 0),

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

comment on table public.profiles is
  '사용자 프로필. auth.users 와 1:1. 기기를 바꿔도 유지되어야 하는 설정이 여기 있다.';

-- ── 가입하면 프로필이 자동으로 생긴다 ──────────────────────────────
--
-- 앱이 따로 만들게 하면 "계정은 있는데 프로필이 없는" 상태가 생긴다.
-- 가입 직후 앱이 죽거나 네트워크가 끊기면 바로 그렇게 된다.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, display_name)
  values (
    new.id,
    -- 구글 로그인이면 이름을 받아 온다. 없으면 기본값.
    coalesce(nullif(new.raw_user_meta_data ->> 'full_name', ''), '러너')
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ── updated_at 자동 갱신 ───────────────────────────────────────────
create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists profiles_touch_updated_at on public.profiles;
create trigger profiles_touch_updated_at
  before update on public.profiles
  for each row execute function public.touch_updated_at();

-- ── 권한 ───────────────────────────────────────────────────────────
--
-- 앱에는 anon 키가 박혀 있다. 그 키는 누구나 꺼내 볼 수 있으므로,
-- 실제 보호는 전부 아래 규칙(RLS)이 한다. RLS 를 켜지 않은 표는
-- 사실상 공개 표다.
alter table public.profiles enable row level security;

-- 프로필은 서로 볼 수 있어야 한다 — 게시글 작성자, 크루 명단, 랭킹.
drop policy if exists profiles_select_all on public.profiles;
create policy profiles_select_all
  on public.profiles for select
  using (true);

-- 고치는 것은 본인만.
drop policy if exists profiles_update_own on public.profiles;
create policy profiles_update_own
  on public.profiles for update
  using ((select auth.uid()) = id)
  with check ((select auth.uid()) = id);

-- INSERT 정책을 두지 않는다. 프로필은 위 트리거만 만든다.
-- DELETE 정책도 두지 않는다. 계정을 지우면 따라 지워진다.
