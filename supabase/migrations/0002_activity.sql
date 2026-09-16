-- 걸음과 러닝 세션.
--
-- 앱은 지금까지 이걸 폰 안에만 두었다. 기기를 바꾸면 통째로 사라졌다는 뜻이다.
-- 온체인으로 가지 않기로 했으므로 이 표가 유일한 보관처다.

-- ── 일별 걸음 ──────────────────────────────────────────────────────
create table if not exists public.daily_steps (
  user_id uuid not null references auth.users on delete cascade,
  -- LocalDate.toEpochDay() 와 같은 값. 시간대 문제를 피하려고 날짜를
  -- 타임스탬프가 아니라 "며칠째"로 센다 — 기기 시간대가 바뀌어도
  -- 어제가 어제로 남는다.
  epoch_day bigint not null,
  steps int not null default 0 check (steps >= 0),
  goal int not null default 8000 check (goal > 0),
  updated_at timestamptz not null default now(),
  primary key (user_id, epoch_day)
);

comment on table public.daily_steps is
  '하루치 걸음 합계. 러닝 세션과 별개로 걸은 것까지 포함한다.';

-- ── 러닝 세션 ──────────────────────────────────────────────────────
create table if not exists public.walk_sessions (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users on delete cascade,

  started_at timestamptz not null,
  ended_at timestamptz not null,
  duration_sec int not null check (duration_sec >= 0),

  steps int not null check (steps >= 0),
  distance_meters double precision not null default 0 check (distance_meters >= 0),
  calories double precision not null default 0 check (calories >= 0),

  -- 시각이 붙은 GPS 경로. 앱의 RunTrack.encode 형식 그대로다
  -- ("위도,경도,시각;위도,경도,시각"). 이게 있어야 "정말 뛰었는가"를
  -- 나중에 다시 판정할 수 있다.
  track text not null default '',

  -- 정산 시점의 값. 나중에 다시 계산할 수 없어서 같이 받는다 —
  -- 사용자는 신발을 갈아신고 크루를 옮긴다.
  boost_bps int not null default 0 check (boost_bps between 0 and 2000),
  party_size int not null default 1 check (party_size between 1 and 20),

  -- 서버 판정. 클라이언트가 못 고치게 앱에는 쓰기 권한을 주지 않는다.
  verdict text not null default 'PENDING'
    check (verdict in ('PENDING', 'CLEAN', 'FLAGGED', 'VOID')),
  verdict_reason text not null default '',

  -- 서버가 계산해 실제로 지급한 SUP. 앱이 보낸 값이 아니다.
  points_awarded numeric(20, 4) not null default 0 check (points_awarded >= 0),

  -- 실제로 적립 대상이 된 걸음. 걸은 걸음(steps)과 다를 수 있다 — 하루 상한에
  -- 걸리면 일부만 인정된다. 상한을 셀 때는 반드시 이 값을 더해야 한다.
  -- steps 를 더하면 인정되지도 않은 걸음이 상한을 깎아 다음 세션이 손해를 본다.
  rewarded_steps int not null default 0 check (rewarded_steps >= 0),

  created_at timestamptz not null default now(),

  constraint walk_sessions_ends_after_start check (ended_at >= started_at)
);

comment on column public.walk_sessions.rewarded_steps is
  '적립 대상이 된 걸음. 하루 상한 계산은 이 값을 더한다.';

comment on column public.walk_sessions.points_awarded is
  '서버가 계산해 지급한 금액. 앱이 보낸 값을 그대로 믿지 않는다 — 앱은 고쳐서 다시 설치할 수 있다.';

-- 같은 세션이 두 번 올라오는 것을 막는다. 앱이 재시도할 때 응답을 못 받고
-- 다시 보내면 중복 적립이 된다. (사용자, 시작시각)이면 충분히 유일하다.
create unique index if not exists walk_sessions_user_started_unique
  on public.walk_sessions (user_id, started_at);

create index if not exists walk_sessions_user_recent
  on public.walk_sessions (user_id, started_at desc);

-- ── 첫 배포 뒤에 추가된 열 ─────────────────────────────────────────
--
-- 위의 create table 은 "없으면 만든다"라서, 이미 표가 있는 프로젝트는
-- 통째로 건너뛴다. 나중에 늘어난 열은 여기서 따로 붙여야 그런 프로젝트에도
-- 들어간다. setup.sql 을 다시 붙여넣어도 안전한 이유다.

-- 정산 시점에 신고 있던 신발의 종족. 종족 랭킹의 재료다.
alter table public.walk_sessions
  add column if not exists faction text not null default '';

-- 경로에서 서버가 직접 계산한 최고 속도. 앱이 보낸 값이 아니다 —
-- 속도 랭킹이 있는 이상, 앱이 말하는 속도를 믿으면 랭킹은 타자 연습이 된다.
alter table public.walk_sessions
  add column if not exists top_speed_kmh double precision not null default 0;

do $$
begin
  if not exists (
    select 1 from pg_constraint
     where conrelid = 'public.walk_sessions'::regclass
       and conname = 'walk_sessions_faction_known'
  ) then
    alter table public.walk_sessions
      add constraint walk_sessions_faction_known
      check (faction in ('', 'FIRE', 'WATER', 'LIGHTNING', 'WIND'));
  end if;
end $$;

comment on column public.walk_sessions.top_speed_kmh is
  '서버가 GPS 경로에서 직접 계산한 구간 최고 속도. 경로가 없으면 0.';

-- ── 권한 ───────────────────────────────────────────────────────────
alter table public.daily_steps enable row level security;
alter table public.walk_sessions enable row level security;

-- 걸음은 본인 것만 읽고 쓴다.
drop policy if exists daily_steps_own on public.daily_steps;
create policy daily_steps_own
  on public.daily_steps for all
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

-- 세션은 본인 것만 읽는다.
drop policy if exists walk_sessions_select_own on public.walk_sessions;
create policy walk_sessions_select_own
  on public.walk_sessions for select
  using ((select auth.uid()) = user_id);

-- **INSERT · UPDATE · DELETE 정책을 주지 않는다.**
--
-- 세션을 앱이 직접 넣게 하면 걸음 수를 마음대로 적어 보낼 수 있다.
-- 세션은 반드시 0003 의 record_session() 을 통해서만 들어온다 —
-- 그 함수가 걸음 수를 검사하고 적립액을 직접 계산한다.
