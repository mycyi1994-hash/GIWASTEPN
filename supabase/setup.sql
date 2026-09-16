-- ════════════════════════════════════════════════════════════════════
--  StepUp 서버 스키마 — 전체 설치
--
--  Supabase 대시보드 → SQL Editor 에 이 파일 전체를 붙여넣고 Run 하세요.
--  한 번에 다 만들어집니다.
--
--  끝나면 왼쪽 Table Editor 에 표가 보입니다.
--
--  이 파일은 supabase/migrations/ 의 파일들을 순서대로 이어 붙인 것입니다.
--  내용을 고칠 때는 그쪽을 고치고 scripts/build-setup-sql.py 로 다시 만드세요.
-- ════════════════════════════════════════════════════════════════════

-- 이 파일은 몇 번을 다시 붙여넣어도 안전합니다. 이미 있는 것은 건너뛰고,
-- 달라진 규칙만 새로 씁니다. 기록은 지워지지 않습니다.

begin;

-- "없어서 건너뛴다"는 안내는 처음 설치할 때 잔뜩 나오는데, 문제가 아닌데도
-- 문제처럼 보입니다. 경고 이상만 보여 줍니다.
set local client_min_messages = warning;

-- ══════════════════════════════════════════════════════════════════
-- 0001_profiles.sql
-- ══════════════════════════════════════════════════════════════════

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

-- ══════════════════════════════════════════════════════════════════
-- 0002_activity.sql
-- ══════════════════════════════════════════════════════════════════

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

-- ══════════════════════════════════════════════════════════════════
-- 0003_ledger.sql
-- ══════════════════════════════════════════════════════════════════

-- SUP 원장 — 이 파일이 v1 의 심장이다.
--
-- SUP 는 이제 앱 안 포인트지만, 그렇다고 클라이언트가 마음대로 적어도 되는
-- 값은 아니다. 앱에는 anon 키가 박혀 있고 그 키는 누구나 꺼내 볼 수 있다.
-- 원장에 직접 INSERT 할 수 있으면 잔고는 숫자놀음이 된다.
--
-- 그래서 원장은 **읽기만** 허용하고, 모든 기록은 아래 두 함수로만 들어온다.
--   - record_session()  적립. 서버가 걸음 수를 검사하고 금액을 직접 계산한다.
--   - spend_sup()       소비. 잔고를 확인하고 차감한다.

create table if not exists public.sup_ledger (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users on delete cascade,

  occurred_at timestamptz not null default now(),

  -- 앱의 RewardType 과 같은 값
  kind text not null check (kind in (
    'EARN_WALK', 'EARN_PARTY', 'EARN_EVENT', 'BONUS_GOAL',
    'SPEND_MINT', 'SPEND_UPGRADE', 'SPEND_BOOST'
  )),

  -- 양수 = 적립, 음수 = 사용. 소수 4자리까지 — 0.01 SUP/보 단위를 담기에 충분하고,
  -- double 과 달리 더해도 오차가 쌓이지 않는다. 잔고는 더한 값이므로 중요하다.
  amount numeric(20, 4) not null check (amount <> 0),

  description text not null default '',

  -- 적립이면 어느 세션에서 왔는지. 한 세션은 한 번만 적립된다.
  session_id bigint references public.walk_sessions on delete set null,

  created_at timestamptz not null default now()
);

comment on table public.sup_ledger is
  'SUP 적립·사용 원장. 잔고는 이 표의 합이다. 직접 쓰기는 막혀 있고 함수로만 들어온다.';

create index if not exists sup_ledger_user_recent on public.sup_ledger (user_id, occurred_at desc);

-- 한 세션이 두 번 적립되는 것을 막는다.
create unique index if not exists sup_ledger_session_once
  on public.sup_ledger (session_id)
  where session_id is not null;

-- ── 잔고 ───────────────────────────────────────────────────────────
--
-- 표가 아니라 합계로 둔다. 잔고를 따로 들고 있으면 원장과 어긋날 수 있고,
-- 어긋났을 때 어느 쪽이 맞는지 알 방법이 없다. 합이 곧 잔고라면 어긋날 수가 없다.
create or replace view public.sup_balances
with (security_invoker = true) as
  select user_id, coalesce(sum(amount), 0)::numeric(20, 4) as balance
    from public.sup_ledger
   group by user_id;

comment on view public.sup_balances is
  '원장의 합. 잔고를 따로 저장하지 않는 이유는 어긋날 수 없게 하기 위해서다.';

-- ── 경제 규칙 (앱의 RewardEconomy 와 같은 값) ──────────────────────
--
-- 앱과 서버 양쪽에 같은 숫자가 있는 것은 좋지 않지만, 서버가 앱을 믿지 않으려면
-- 서버도 계산할 줄 알아야 한다. 값이 어긋나면 앱이 보여준 금액과 실제 적립이
-- 달라지므로, 앱의 RewardEconomy.kt 를 고치면 여기도 고친다.
create schema if not exists economy;

create or replace function economy.points_per_step() returns numeric
  language sql immutable as $$ select 0.01::numeric $$;

create or replace function economy.steps_per_energy() returns int
  language sql immutable as $$ select 600 $$;

create or replace function economy.base_max_energy() returns numeric
  language sql immutable as $$ select 10::numeric $$;

-- 하루에 적립 대상이 될 수 있는 걸음의 절대 상한.
-- 에너지 규칙이 이미 더 낮게 묶지만, 규칙이 틀려도 피해가 이 선을 넘지 않게 하는
-- 마지막 방어선이다.
create or replace function economy.max_daily_steps() returns int
  language sql immutable as $$ select 48000 $$;

-- 파티런 보너스 — 본인 제외 1명당 +10%, 5명까지
create or replace function economy.party_multiplier(party_size int) returns numeric
  language sql immutable as $$
  select 1 + 0.10 * least(greatest(coalesce(party_size, 1) - 1, 0), 5)
$$;

-- 스니커즈 부스트 — bps 를 배율로. 1780 → 1.178
create or replace function economy.boost_multiplier(boost_bps int) returns numeric
  language sql immutable as $$
  select 1 + least(greatest(coalesce(boost_bps, 0), 0), 2000)::numeric / 10000
$$;

-- ── GPS 경로에서 속도 읽기 ─────────────────────────────────────────
--
-- 속도 랭킹이 있는 이상, 앱이 말하는 속도를 그대로 받으면 그 랭킹은 달리기가
-- 아니라 타자 실력을 재게 된다. 경로는 이미 올라오니 서버가 직접 잰다.

create or replace function economy.haversine_m(
  lat1 double precision, lng1 double precision,
  lat2 double precision, lng2 double precision
) returns double precision
language sql immutable as $$
  select 2 * 6371000 * asin(least(1, sqrt(
    power(sin(radians(lat2 - lat1) / 2), 2)
      + cos(radians(lat1)) * cos(radians(lat2))
      * power(sin(radians(lng2 - lng1) / 2), 2)
  )))
$$;

-- 한 구간으로 볼 최소 시간(초). 1초짜리 구간으로 재면 GPS 가 몇 미터 튀는
-- 것만으로 시속 수십 km 가 나온다. 10초면 그 흔들림이 묻힌다.
create or replace function economy.speed_window_sec() returns int
  language sql immutable as $$ select 10 $$;

-- 이 이상은 사람의 이동으로 보지 않는다. 사람이 낸 최고 기록이 약 37km/h 이므로
-- 60 은 넉넉히 위다 — 여기 걸리는 구간은 달린 게 아니라 튄 것이거나 탄 것이다.
create or replace function economy.speed_glitch_kmh() returns double precision
  language sql immutable as $$ select 60::double precision $$;

-- 랭킹에 올릴 수 있는 상한. 튀지 않았더라도 세계 기록 위는 기록으로 받지 않는다.
create or replace function economy.speed_record_cap_kmh() returns double precision
  language sql immutable as $$ select 45::double precision $$;

drop function if exists economy.track_speed_stats(text);

/*
 * 경로에서 구간 최고 속도와 "말이 안 되는 구간"의 비율을 뽑는다.
 *
 * 둘을 함께 돌려주는 이유는 판단이 다르기 때문이다. 터널이나 빌딩 사이에서
 * 좌표 하나가 튀는 것은 흔한 일이라 그 구간 하나로 세션 전체를 버리면 정직한
 * 기록이 사라진다. 하지만 구간 대부분이 말이 안 되면 그건 GPS 오류가 아니라
 * 그 사람이 달리지 않은 것이다.
 */
create or replace function economy.track_speed_stats(p_track text)
returns table (top_speed_kmh double precision, glitch_ratio double precision)
language plpgsql immutable as $$
declare
  v_num constant text := '^-?[0-9]+(\.[0-9]+)?$';
  v_chunk text;
  v_parts text[];
  v_lat double precision;
  v_lng double precision;
  v_at bigint;
  -- 비교 기준점. 여기서부터 speed_window_sec 초가 지나야 한 구간으로 친다.
  v_blat double precision;
  v_blng double precision;
  v_bat bigint;
  v_have_base boolean := false;
  v_dt double precision;
  v_kmh double precision;
  v_best double precision := 0;
  v_windows int := 0;
  v_glitches int := 0;
begin
  if p_track is null or p_track = '' then
    return query select 0::double precision, 0::double precision;
    return;
  end if;

  foreach v_chunk in array string_to_array(p_track, ';') loop
    v_parts := string_to_array(v_chunk, ',');
    -- 깨진 조각은 건너뛴다. 한 점이 깨졌다고 나머지 경로를 버릴 이유는 없다.
    continue when v_parts is null or array_length(v_parts, 1) <> 3;
    continue when v_parts[1] !~ v_num or v_parts[2] !~ v_num or v_parts[3] !~ '^[0-9]+$';

    v_lat := v_parts[1]::double precision;
    v_lng := v_parts[2]::double precision;
    v_at := v_parts[3]::bigint;  -- epoch 밀리초. 앱의 RunTrack 과 같은 단위다.

    if not v_have_base then
      v_blat := v_lat; v_blng := v_lng; v_bat := v_at; v_have_base := true;
      continue;
    end if;

    v_dt := (v_at - v_bat) / 1000.0;
    -- 시계가 거꾸로 간 점은 기준을 다시 잡는다.
    if v_dt <= 0 then
      v_blat := v_lat; v_blng := v_lng; v_bat := v_at;
      continue;
    end if;
    continue when v_dt < economy.speed_window_sec();

    v_kmh := economy.haversine_m(v_blat, v_blng, v_lat, v_lng) / v_dt * 3.6;
    v_windows := v_windows + 1;
    if v_kmh > economy.speed_glitch_kmh() then
      v_glitches := v_glitches + 1;
    elsif v_kmh > v_best then
      v_best := v_kmh;
    end if;

    v_blat := v_lat; v_blng := v_lng; v_bat := v_at;
  end loop;

  return query select
    least(v_best, economy.speed_record_cap_kmh()),
    case when v_windows = 0 then 0::double precision
         else v_glitches::double precision / v_windows end;
end;
$$;

comment on function economy.track_speed_stats is
  '경로에서 구간 최고 속도와 비정상 구간 비율을 뽑는다. 앱이 보낸 속도는 쓰지 않는다.';

-- ── 지난 버전의 함수를 치운다 ──────────────────────────────────────
--
-- 인자가 늘면 create or replace 로는 못 바꾸고 같은 이름이 둘이 된다. 그러면
-- 호출이 어느 쪽으로 갈지 모호해져 앱이 조용히 옛 규칙으로 적립될 수 있다.
do $$
declare r record;
begin
  for r in
    select oid::regprocedure as sig
      from pg_proc
     where pronamespace = 'public'::regnamespace
       and proname in ('record_session', 'spend_sup')
  loop
    execute 'drop function ' || r.sig;
  end loop;
end $$;

-- ── 적립 ───────────────────────────────────────────────────────────
--
-- 앱이 세션을 올리면 이 함수가 받는다. 앱이 계산한 금액은 받지도 않는다 —
-- 걸음 수와 정산 시점 값만 받아서 서버가 직접 계산한다.
create or replace function public.record_session(
  p_started_at timestamptz,
  p_ended_at timestamptz,
  p_steps int,
  p_duration_sec int,
  p_track text default '',
  p_boost_bps int default 0,
  p_party_size int default 1,
  -- 정산 시점에 신고 있던 신발의 종족. 종족 랭킹에 쌓인다.
  p_faction text default ''
)
returns table (
  session_id bigint,
  verdict text,
  points_awarded numeric,
  balance numeric
)
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_day bigint;
  v_already_today int;
  v_rewardable int;
  v_points numeric(20, 4);
  v_verdict text := 'CLEAN';
  v_reason text := '';
  v_session_id bigint;
  v_kind text;
  v_elapsed int;
  v_faction text;
  v_top_speed double precision := 0;
  v_glitch double precision := 0;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  -- ── 형식 검사 ──
  if p_steps is null or p_steps < 0 then
    raise exception '걸음 수가 올바르지 않습니다' using errcode = '22023';
  end if;
  if p_ended_at < p_started_at then
    raise exception '종료 시각이 시작보다 빠릅니다' using errcode = '22023';
  end if;
  -- 미래에서 온 세션은 받지 않는다. 기기 시계를 앞당기면 내일 몫을 오늘 받는다.
  if p_ended_at > now() + interval '5 minutes' then
    raise exception '종료 시각이 미래입니다' using errcode = '22023';
  end if;

  v_elapsed := greatest(coalesce(p_duration_sec, 0), 0);
  v_day := floor(extract(epoch from p_started_at) / 86400)::bigint;
  v_faction := case when coalesce(p_faction, '') in ('FIRE', 'WATER', 'LIGHTNING', 'WIND')
                    then p_faction else '' end;

  -- 경로에서 속도를 직접 잰다. 경로가 없으면 0 이고, 그 세션은 속도 랭킹에
  -- 올라가지 않는다 — 잴 수 없는 기록은 기록이 아니다.
  select t.top_speed_kmh, t.glitch_ratio
    into v_top_speed, v_glitch
    from economy.track_speed_stats(coalesce(p_track, '')) t;

  -- ── 판정 ──
  --
  -- 사람의 이동으로 보기 어려운 세션은 적립하지 않는다. 여기서는 케이던스만
  -- 본다 — GPS 경로 검사는 좌표를 하나씩 훑어야 해서 Edge Function 쪽이 맞고,
  -- 케이던스는 나눗셈 한 번이라 여기서 끝난다.
  if v_elapsed >= 60 and p_steps::numeric * 60 / v_elapsed > 240 then
    v_verdict := 'VOID';
    v_reason := '케이던스가 사람 범위를 벗어납니다';

  -- 구간 대부분이 사람 속도를 넘으면 GPS 오류가 아니라 타고 간 것이다.
  -- 절반이라는 선은 넉넉하다 — 도심에서 좌표가 튀는 일은 흔해도, 구간의
  -- 절반이 시속 60km 를 넘는 일은 흔하지 않다.
  elsif v_glitch > 0.5 then
    v_verdict := 'VOID';
    v_reason := '이동 속도가 사람 범위를 벗어납니다';
  end if;

  -- ── 적립 대상 걸음 ──
  if v_verdict = 'VOID' then
    v_rewardable := 0;
  else
    -- 오늘 이미 **적립된** 걸음을 빼고 남은 만큼만 인정한다.
    -- 걸은 걸음(steps)이 아니라 적립된 걸음(rewarded_steps)이어야 한다 —
    -- 상한에 걸려 인정되지 않은 몫까지 깎으면 다음 세션이 손해를 본다.
    select coalesce(sum(s.rewarded_steps), 0) into v_already_today
      from public.walk_sessions s
     where s.user_id = v_user
       and s.verdict in ('CLEAN', 'FLAGGED')
       and floor(extract(epoch from s.started_at) / 86400)::bigint = v_day;

    v_rewardable := greatest(
      least(p_steps, economy.max_daily_steps() - v_already_today),
      0
    );

    if v_rewardable < p_steps then
      v_verdict := 'FLAGGED';
      v_reason := '하루 적립 상한에 걸렸습니다';
    end if;
  end if;

  v_points := round(
    v_rewardable * economy.points_per_step()
      * economy.party_multiplier(p_party_size)
      * economy.boost_multiplier(p_boost_bps),
    4
  );

  -- ── 기록 ──
  --
  -- 같은 세션을 다시 올리면(앱이 응답을 못 받고 재시도) 새로 적립하지 않고
  -- 원래 결과를 그대로 돌려준다. 재시도가 중복 적립이 되면 안 된다.
  insert into public.walk_sessions (
    user_id, started_at, ended_at, duration_sec, steps,
    distance_meters, calories, track, boost_bps, party_size,
    faction, top_speed_kmh,
    verdict, verdict_reason, points_awarded, rewarded_steps
  )
  values (
    v_user, p_started_at, p_ended_at, v_elapsed, p_steps,
    p_steps * 0.762, p_steps * 0.04, coalesce(p_track, ''), p_boost_bps, p_party_size,
    v_faction, case when v_verdict = 'VOID' then 0 else v_top_speed end,
    v_verdict, v_reason, v_points, v_rewardable
  )
  on conflict (user_id, started_at) do nothing
  returning id into v_session_id;

  if v_session_id is null then
    -- 이미 처리된 세션이다. 원래 결과를 돌려준다.
    select s.id, s.verdict, s.points_awarded
      into v_session_id, v_verdict, v_points
      from public.walk_sessions s
     where s.user_id = v_user and s.started_at = p_started_at;

    return query
      select v_session_id, v_verdict, v_points,
             coalesce((select b.balance from public.sup_balances b where b.user_id = v_user), 0);
    return;
  end if;

  if v_points > 0 then
    v_kind := case when coalesce(p_party_size, 1) > 1 then 'EARN_PARTY' else 'EARN_WALK' end;
    insert into public.sup_ledger (user_id, kind, amount, description, session_id, occurred_at)
    values (v_user, v_kind, v_points, format('러닝 세션 적립 (%s보)', v_rewardable),
            v_session_id, p_ended_at);
  end if;

  -- 랭킹 재료 갱신.
  -- VOID 판정을 받은 세션은 아무것도 남기지 않는다 — 적립을 막아 놓고
  -- 기록만 올려 주면 속도 랭킹은 그쪽으로 뚫린다.
  if v_verdict <> 'VOID' then
    update public.profiles
       set lifetime_km = lifetime_km + (p_steps * 0.762 / 1000),
           top_speed_kmh = greatest(top_speed_kmh, v_top_speed)
     where id = v_user;
  end if;

  return query
    select v_session_id, v_verdict, v_points,
           coalesce((select b.balance from public.sup_balances b where b.user_id = v_user), 0);
end;
$$;

comment on function public.record_session is
  '러닝 세션을 기록하고 적립액을 서버가 직접 계산한다. 앱이 계산한 금액은 받지 않는다.';

-- ── 소비 ───────────────────────────────────────────────────────────
create or replace function public.spend_sup(
  p_kind text,
  p_amount numeric,
  p_description text default ''
)
returns numeric
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_balance numeric(20, 4);
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_kind not in ('SPEND_MINT', 'SPEND_UPGRADE', 'SPEND_BOOST') then
    raise exception '소비 종류가 올바르지 않습니다: %', p_kind using errcode = '22023';
  end if;
  if p_amount is null or p_amount <= 0 then
    raise exception '금액이 올바르지 않습니다' using errcode = '22023';
  end if;

  -- 잔고를 세는 동안 다른 요청이 끼어들지 못하게 이 사용자의 원장 행을 잠근다.
  -- 잠그지 않으면 두 요청이 동시에 "잔고 충분"을 보고 둘 다 통과해 잔고가
  -- 음수가 된다 — 지갑이 없어도 이중지불은 일어난다.
  perform 1 from public.sup_ledger where user_id = v_user for update;

  select coalesce(sum(amount), 0) into v_balance
    from public.sup_ledger where user_id = v_user;

  if v_balance < p_amount then
    raise exception 'SUP가 부족합니다 (보유 %, 필요 %)', v_balance, p_amount
      using errcode = '23514';
  end if;

  insert into public.sup_ledger (user_id, kind, amount, description)
  values (v_user, p_kind, -p_amount, coalesce(p_description, ''));

  return v_balance - p_amount;
end;
$$;

comment on function public.spend_sup is
  'SUP 를 차감한다. 잔고를 확인하고, 동시 요청이 겹쳐 음수가 되지 않게 잠근다.';

-- ── 권한 ───────────────────────────────────────────────────────────
alter table public.sup_ledger enable row level security;

-- 본인 원장만 읽는다.
drop policy if exists sup_ledger_select_own on public.sup_ledger;
create policy sup_ledger_select_own
  on public.sup_ledger for select
  using ((select auth.uid()) = user_id);

-- 쓰기 정책은 두지 않는다. 위 두 함수(security definer)만 쓴다.
--
-- 정책이 없으면 RLS 가 이미 막지만, 권한 자체도 회수해 둔다. 나중에 누군가
-- 실수로 허용 정책을 하나 붙여도 권한이 없으면 여전히 못 쓴다. 잔고가 걸린
-- 표라 방어를 한 겹 더 둔다.
revoke insert, update, delete on public.sup_ledger from anon, authenticated;
revoke insert, update, delete on public.walk_sessions from anon, authenticated;

grant execute on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text)
  to authenticated;
grant execute on function public.spend_sup(text, numeric, text) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0004_community.sql
-- ══════════════════════════════════════════════════════════════════

-- 커뮤니티 — 크루, 게시판, 댓글, 코스.
--
-- 지금까지 이 데이터는 전부 폰 안에만 있었다. 그래서 "커뮤니티"라고 부르면서도
-- 실제로는 혼자 쓰는 메모장이었다 — 내가 쓴 글을 아무도 볼 수 없었다.
-- 이 파일이 그걸 진짜 공용 공간으로 바꾼다.
--
-- 앱의 Room 스키마와 한 군데가 다르다. 폰 안에서는 `liked`, `joined`, `mine`
-- 같은 값이 글에 붙은 칸이었지만, 서버에서는 그럴 수 없다. 같은 글이라도
-- 누가 보느냐에 따라 답이 달라지기 때문이다. 그래서 그 값들은 칸이 아니라
-- 별도의 표(post_likes, flash_participants)가 되고, 보는 사람 기준으로
-- 계산해서 내려준다.

-- ════════════════════════════════════════════════════════════════════
--  크루
-- ════════════════════════════════════════════════════════════════════

create table if not exists public.crews (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users on delete cascade,

  name text not null check (length(name) between 1 and 40),
  monogram text not null default '' check (length(monogram) <= 4),
  tagline text not null default '' check (length(tagline) <= 120),
  area text not null default '' check (length(area) <= 60),

  -- 활동 중심지. "몇 km 떨어져 있나"는 보는 사람 위치가 있어야 나오므로
  -- 서버는 좌표만 들고 있고 거리는 앱이 계산한다.
  lat double precision,
  lng double precision,

  created_at timestamptz not null default now()
);

comment on table public.crews is '러닝 크루. 만든 사람이 자동으로 첫 멤버가 된다.';

create table if not exists public.crew_members (
  crew_id uuid not null references public.crews on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  role text not null default 'MEMBER' check (role in ('OWNER', 'MEMBER')),
  joined_at timestamptz not null default now(),
  primary key (crew_id, user_id)
);

create index if not exists crew_members_user on public.crew_members (user_id);

-- 크루를 만든 사람은 그 자리에서 멤버가 된다.
--
-- 앱이 만들기와 가입을 따로 호출하게 두면, 둘 사이에서 앱이 죽었을 때
-- "주인이 멤버가 아닌 크루"가 남는다. 그 크루는 주인조차 글을 못 쓴다.
create or replace function public.handle_new_crew()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.crew_members (crew_id, user_id, role)
  values (new.id, new.owner_id, 'OWNER')
  on conflict do nothing;
  return new;
end;
$$;

drop trigger if exists on_crew_created on public.crews;
create trigger on_crew_created
  after insert on public.crews
  for each row execute function public.handle_new_crew();

-- ════════════════════════════════════════════════════════════════════
--  차단과 신고
--
--  사용자가 글을 쓸 수 있는 앱은 스토어 정책상 신고와 차단 수단이 있어야
--  한다. 그보다 먼저, 이게 없으면 한 사람이 공간 전체를 망칠 수 있다.
-- ════════════════════════════════════════════════════════════════════

create table if not exists public.user_blocks (
  blocker_id uuid not null references auth.users on delete cascade,
  blocked_id uuid not null references auth.users on delete cascade,
  created_at timestamptz not null default now(),
  primary key (blocker_id, blocked_id),
  constraint user_blocks_not_self check (blocker_id <> blocked_id)
);

comment on table public.user_blocks is
  '차단 목록. 차단하면 그 사람의 글·댓글이 내 화면에서 사라진다. 상대에게는 알리지 않는다.';

create table if not exists public.content_reports (
  id bigint generated always as identity primary key,
  reporter_id uuid not null references auth.users on delete cascade,
  target_type text not null check (target_type in ('POST', 'COMMENT', 'CREW', 'COURSE', 'USER')),
  -- 대상 id. 표마다 자료형이 달라(uuid/bigint) 문자열로 받는다.
  target_id text not null,
  reason text not null check (reason in ('SPAM', 'ABUSE', 'SEXUAL', 'DANGER', 'FRAUD', 'OTHER')),
  note text not null default '' check (length(note) <= 1000),
  status text not null default 'OPEN' check (status in ('OPEN', 'REVIEWED', 'ACTIONED', 'DISMISSED')),
  created_at timestamptz not null default now(),
  -- 같은 사람이 같은 대상을 반복 신고해도 한 건이다.
  unique (reporter_id, target_type, target_id)
);

comment on table public.content_reports is
  '신고 접수함. 처리는 사람이 대시보드에서 한다 — 자동 삭제는 오판했을 때 되돌릴 수 없다.';

-- 차단했는지. 정책과 뷰 양쪽에서 쓰므로 함수로 둔다.
create or replace function public.is_blocked(p_user uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.user_blocks b
     where b.blocker_id = auth.uid() and b.blocked_id = p_user
  )
$$;

-- ════════════════════════════════════════════════════════════════════
--  게시판
-- ════════════════════════════════════════════════════════════════════

create table if not exists public.posts (
  id bigint generated always as identity primary key,
  author_id uuid not null references auth.users on delete cascade,

  category text not null check (category in ('FLASH', 'FREE', 'TIP')),

  -- 비어 있으면(null) 전체 게시판, 값이 있으면 그 크루만 보는 게시판.
  crew_id uuid references public.crews on delete cascade,

  title text not null check (length(title) between 1 and 120),
  body text not null default '' check (length(body) <= 4000),

  -- ── 번개러닝 전용 ──
  place text not null default '' check (length(place) <= 80),
  distance_km double precision not null default 0 check (distance_km >= 0),
  meet_at timestamptz,
  capacity int not null default 0 check (capacity between 0 and 200),

  created_at timestamptz not null default now()
);

create index if not exists posts_feed on public.posts (crew_id, created_at desc);
create index if not exists posts_author on public.posts (author_id);
-- 번개는 "지금부터 가까운 순"으로 보므로 따로 잡아 둔다.
create index if not exists posts_flash_upcoming on public.posts (meet_at)
  where category = 'FLASH';

create table if not exists public.post_likes (
  post_id bigint not null references public.posts on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  created_at timestamptz not null default now(),
  primary key (post_id, user_id)
);

create table if not exists public.flash_participants (
  post_id bigint not null references public.posts on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  joined_at timestamptz not null default now(),
  primary key (post_id, user_id)
);

comment on table public.flash_participants is
  '번개러닝 참가자. 정원을 넘지 않게 join_flash() 로만 들어온다 — 직접 INSERT 는 막혀 있다.';

create table if not exists public.comments (
  id bigint generated always as identity primary key,
  post_id bigint not null references public.posts on delete cascade,
  -- null 이면 최상위 댓글. 앱의 parentId=0 과 같은 뜻이다.
  parent_id bigint references public.comments on delete cascade,
  author_id uuid not null references auth.users on delete cascade,
  body text not null check (length(body) between 1 and 1000),
  created_at timestamptz not null default now()
);

create index if not exists comments_post on public.comments (post_id, created_at);

-- ════════════════════════════════════════════════════════════════════
--  코스
-- ════════════════════════════════════════════════════════════════════

create table if not exists public.courses (
  id bigint generated always as identity primary key,
  owner_id uuid not null references auth.users on delete cascade,

  name text not null check (length(name) between 1 and 60),
  area text not null default '' check (length(area) <= 60),
  distance_km double precision not null default 0 check (distance_km >= 0),
  elevation_m int not null default 0,

  -- 앱의 RunCourse.encode 형식 — "위도,경도;위도,경도". 러닝 경로와 달리
  -- 시각이 없다. 코스는 "언제 지났나"가 아니라 "어디를 지나나"이기 때문이다.
  track text not null default '',

  -- 코스 게시판에 올렸는지. 안 올린 코스는 나만 본다.
  shared boolean not null default false,
  run_count int not null default 0 check (run_count >= 0),

  created_at timestamptz not null default now()
);

create index if not exists courses_shared on public.courses (shared, created_at desc);
create index if not exists courses_owner on public.courses (owner_id);

create table if not exists public.course_likes (
  course_id bigint not null references public.courses on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  created_at timestamptz not null default now(),
  primary key (course_id, user_id)
);

-- ════════════════════════════════════════════════════════════════════
--  누가 무엇을 볼 수 있나
-- ════════════════════════════════════════════════════════════════════

-- 크루 멤버인지. 정책 안에서 여러 번 쓰이므로 함수로 둔다.
-- 크루가 없는 글(전체 게시판)은 누구나 본다.
create or replace function public.is_crew_member(p_crew uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select p_crew is null or exists (
    select 1 from public.crew_members m
     where m.crew_id = p_crew and m.user_id = auth.uid()
  )
$$;

-- 이 글을 볼 수 있는지. 댓글·좋아요 정책이 글의 공개 범위를 따라가야 한다 —
-- 크루 글은 안 보이는데 그 댓글은 보이면 담장에 구멍이 난 것이다.
create or replace function public.can_see_post(p_post bigint)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.posts p
     where p.id = p_post and public.is_crew_member(p.crew_id)
  )
$$;

alter table public.crews enable row level security;
alter table public.crew_members enable row level security;
alter table public.user_blocks enable row level security;
alter table public.content_reports enable row level security;
alter table public.posts enable row level security;
alter table public.post_likes enable row level security;
alter table public.flash_participants enable row level security;
alter table public.comments enable row level security;
alter table public.courses enable row level security;
alter table public.course_likes enable row level security;

-- ── 크루 ──
-- 크루 목록은 가입하기 전에 보여야 한다. 안 보이면 가입할 수가 없다.
drop policy if exists crews_select_all on public.crews;
create policy crews_select_all on public.crews for select using (true);

drop policy if exists crews_insert_own on public.crews;
create policy crews_insert_own on public.crews for insert
  with check ((select auth.uid()) = owner_id);

drop policy if exists crews_update_owner on public.crews;
create policy crews_update_owner on public.crews for update
  using ((select auth.uid()) = owner_id)
  with check ((select auth.uid()) = owner_id);

drop policy if exists crews_delete_owner on public.crews;
create policy crews_delete_owner on public.crews for delete
  using ((select auth.uid()) = owner_id);

-- ── 크루 멤버 ──
drop policy if exists crew_members_select_all on public.crew_members;
create policy crew_members_select_all on public.crew_members for select using (true);

-- 가입은 본인만, 역할은 MEMBER 로만. OWNER 를 직접 넣을 수 있으면
-- 남의 크루에 주인 행세를 하는 행이 생긴다.
drop policy if exists crew_members_join_self on public.crew_members;
create policy crew_members_join_self on public.crew_members for insert
  with check ((select auth.uid()) = user_id and role = 'MEMBER');

-- 탈퇴는 본인만. 주인은 못 나간다 — 나가면 주인 없는 크루가 남는다.
-- 정리하려면 크루를 지워야 한다.
drop policy if exists crew_members_leave_self on public.crew_members;
create policy crew_members_leave_self on public.crew_members for delete
  using ((select auth.uid()) = user_id and role <> 'OWNER');

-- ── 차단·신고 ──
drop policy if exists user_blocks_own on public.user_blocks;
create policy user_blocks_own on public.user_blocks for all
  using ((select auth.uid()) = blocker_id)
  with check ((select auth.uid()) = blocker_id);

-- 신고는 넣을 수만 있고, 내가 넣은 것만 보인다. 처리 상태를 앱이 바꿀 수는 없다.
drop policy if exists content_reports_insert_own on public.content_reports;
create policy content_reports_insert_own on public.content_reports for insert
  with check ((select auth.uid()) = reporter_id);

drop policy if exists content_reports_select_own on public.content_reports;
create policy content_reports_select_own on public.content_reports for select
  using ((select auth.uid()) = reporter_id);

revoke update, delete on public.content_reports from anon, authenticated;

-- ── 게시글 ──
drop policy if exists posts_select_visible on public.posts;
create policy posts_select_visible on public.posts for select
  using (public.is_crew_member(crew_id));

drop policy if exists posts_insert_own on public.posts;
create policy posts_insert_own on public.posts for insert
  with check (
    (select auth.uid()) = author_id
    -- 안 들어간 크루의 게시판에는 쓸 수 없다.
    and (crew_id is null or public.is_crew_member(crew_id))
  );

drop policy if exists posts_update_own on public.posts;
create policy posts_update_own on public.posts for update
  using ((select auth.uid()) = author_id)
  with check ((select auth.uid()) = author_id);

drop policy if exists posts_delete_own on public.posts;
create policy posts_delete_own on public.posts for delete
  using ((select auth.uid()) = author_id);

-- ── 좋아요 ──
-- 누가 눌렀는지는 모두 볼 수 있다. 개수를 세려면 그래야 하고, 좋아요는
-- 원래 드러내는 행동이다.
drop policy if exists post_likes_select_all on public.post_likes;
create policy post_likes_select_all on public.post_likes for select using (true);

drop policy if exists post_likes_insert_own on public.post_likes;
create policy post_likes_insert_own on public.post_likes for insert
  with check ((select auth.uid()) = user_id and public.can_see_post(post_id));

drop policy if exists post_likes_delete_own on public.post_likes;
create policy post_likes_delete_own on public.post_likes for delete
  using ((select auth.uid()) = user_id);

-- ── 번개 참가 ──
-- 읽기만 열어 둔다. 넣고 빼는 것은 아래 join_flash / leave_flash 만 한다 —
-- 정원은 직접 INSERT 로는 지킬 수 없다.
drop policy if exists flash_participants_select_all on public.flash_participants;
create policy flash_participants_select_all on public.flash_participants for select using (true);

revoke insert, update, delete on public.flash_participants from anon, authenticated;

-- ── 댓글 ──
drop policy if exists comments_select_visible on public.comments;
create policy comments_select_visible on public.comments for select
  using (public.can_see_post(post_id));

drop policy if exists comments_insert_own on public.comments;
create policy comments_insert_own on public.comments for insert
  with check ((select auth.uid()) = author_id and public.can_see_post(post_id));

drop policy if exists comments_delete_own on public.comments;
create policy comments_delete_own on public.comments for delete
  using ((select auth.uid()) = author_id);

-- 댓글은 고칠 수 없다. 대화가 오간 뒤에 앞말이 바뀌면 뒷말이 뜻을 잃는다.

-- ── 코스 ──
drop policy if exists courses_select_shared_or_own on public.courses;
create policy courses_select_shared_or_own on public.courses for select
  using (shared or (select auth.uid()) = owner_id);

drop policy if exists courses_insert_own on public.courses;
create policy courses_insert_own on public.courses for insert
  with check ((select auth.uid()) = owner_id);

drop policy if exists courses_update_own on public.courses;
create policy courses_update_own on public.courses for update
  using ((select auth.uid()) = owner_id)
  with check ((select auth.uid()) = owner_id);

drop policy if exists courses_delete_own on public.courses;
create policy courses_delete_own on public.courses for delete
  using ((select auth.uid()) = owner_id);

drop policy if exists course_likes_select_all on public.course_likes;
create policy course_likes_select_all on public.course_likes for select using (true);

drop policy if exists course_likes_insert_own on public.course_likes;
create policy course_likes_insert_own on public.course_likes for insert
  with check ((select auth.uid()) = user_id);

drop policy if exists course_likes_delete_own on public.course_likes;
create policy course_likes_delete_own on public.course_likes for delete
  using ((select auth.uid()) = user_id);

-- ════════════════════════════════════════════════════════════════════
--  번개러닝 참가 — 정원이 있는 일은 함수로만
-- ════════════════════════════════════════════════════════════════════

create or replace function public.join_flash(p_post_id bigint)
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_capacity int;
  v_meet_at timestamptz;
  v_crew uuid;
  v_count int;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  -- 이 글을 잠근다. 두 사람이 마지막 한 자리를 동시에 노리면, 잠그지 않는 한
  -- 둘 다 "아직 자리 있음"을 보고 둘 다 들어간다. 정원 10명인 모임에 11명이
  -- 나타나는 일은 그렇게 생긴다.
  select p.capacity, p.meet_at, p.crew_id
    into v_capacity, v_meet_at, v_crew
    from public.posts p
   where p.id = p_post_id and p.category = 'FLASH'
     for update;

  if not found then
    raise exception '번개러닝 글을 찾을 수 없습니다' using errcode = 'P0002';
  end if;
  if not public.is_crew_member(v_crew) then
    raise exception '이 크루의 멤버가 아닙니다' using errcode = '42501';
  end if;
  if v_meet_at is not null and v_meet_at < now() then
    raise exception '이미 지난 모임입니다' using errcode = '23514';
  end if;

  insert into public.flash_participants (post_id, user_id)
  values (p_post_id, v_user)
  on conflict do nothing;

  select count(*) into v_count
    from public.flash_participants f where f.post_id = p_post_id;

  -- 정원 0 은 제한 없음이다.
  if v_capacity > 0 and v_count > v_capacity then
    raise exception '정원이 찼습니다 (%명)', v_capacity using errcode = '23514';
  end if;

  return v_count;
end;
$$;

comment on function public.join_flash is
  '번개러닝에 참가한다. 정원을 넘지 않게 글을 잠그고 센다.';

create or replace function public.leave_flash(p_post_id bigint)
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_count int;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  delete from public.flash_participants
   where post_id = p_post_id and user_id = v_user;

  select count(*) into v_count
    from public.flash_participants f where f.post_id = p_post_id;
  return v_count;
end;
$$;

grant execute on function public.join_flash(bigint) to authenticated;
grant execute on function public.leave_flash(bigint) to authenticated;

-- ════════════════════════════════════════════════════════════════════
--  앱이 읽는 모양
--
--  앱은 글 하나를 그릴 때 좋아요 수, 댓글 수, 참가 인원, 그리고 "내가"
--  눌렀는지까지 필요하다. 그걸 앱이 매번 따로 물으면 목록 한 번에 요청이
--  수십 개가 된다. 여기서 한 줄로 만들어 둔다.
--
--  security_invoker = true 는 "이 뷰를 읽는 사람의 권한으로 본다"는 뜻이다.
--  이게 없으면 뷰가 RLS 를 통째로 우회해, 안 보여야 할 크루 글이 뷰를 통해
--  새어 나간다.
-- ════════════════════════════════════════════════════════════════════

drop view if exists public.post_feed;
create view public.post_feed
with (security_invoker = true) as
  select
    p.id,
    p.category,
    p.crew_id,
    p.author_id,
    coalesce(pr.display_name, '러너') as author,
    p.title,
    p.body,
    p.place,
    p.distance_km,
    p.meet_at,
    p.capacity,
    p.created_at,
    (select count(*) from public.post_likes l where l.post_id = p.id) as likes,
    (select count(*) from public.comments c where c.post_id = p.id) as comment_count,
    (select count(*) from public.flash_participants f where f.post_id = p.id) as joined_count,
    exists (
      select 1 from public.post_likes l
       where l.post_id = p.id and l.user_id = auth.uid()
    ) as liked,
    exists (
      select 1 from public.flash_participants f
       where f.post_id = p.id and f.user_id = auth.uid()
    ) as joined,
    p.author_id = auth.uid() as mine
  from public.posts p
  left join public.profiles pr on pr.id = p.author_id
  -- 차단한 사람의 글은 내 화면에서 사라진다. 상대는 이를 알 수 없다 —
  -- 알리면 차단이 다툼의 시작이 된다.
  where not public.is_blocked(p.author_id);

comment on view public.post_feed is
  '게시글 목록. 좋아요·댓글·참가 수와 "내가 눌렀는지"까지 한 줄에 담는다.';

drop view if exists public.comment_feed;
create view public.comment_feed
with (security_invoker = true) as
  select
    c.id,
    c.post_id,
    coalesce(c.parent_id, 0) as parent_id,
    c.author_id,
    coalesce(pr.display_name, '러너') as author,
    c.body,
    c.created_at,
    c.author_id = auth.uid() as mine
  from public.comments c
  left join public.profiles pr on pr.id = c.author_id
  where not public.is_blocked(c.author_id);

drop view if exists public.crew_feed;
create view public.crew_feed
with (security_invoker = true) as
  select
    c.id,
    c.owner_id,
    c.name,
    c.monogram,
    c.tagline,
    c.area,
    c.lat,
    c.lng,
    c.created_at,
    (select count(*) from public.crew_members m where m.crew_id = c.id) as member_count,
    exists (
      select 1 from public.crew_members m
       where m.crew_id = c.id and m.user_id = auth.uid()
    ) as joined,
    c.owner_id = auth.uid() as owned
  from public.crews c;

drop view if exists public.course_feed;
create view public.course_feed
with (security_invoker = true) as
  select
    c.id,
    c.owner_id,
    coalesce(pr.display_name, '러너') as author,
    c.name,
    c.area,
    c.distance_km,
    c.elevation_m,
    c.track,
    c.shared,
    c.run_count,
    c.created_at,
    (select count(*) from public.course_likes l where l.course_id = c.id) as likes,
    exists (
      select 1 from public.course_likes l
       where l.course_id = c.id and l.user_id = auth.uid()
    ) as liked,
    c.owner_id = auth.uid() as mine
  from public.courses c
  left join public.profiles pr on pr.id = c.owner_id
  where not public.is_blocked(c.owner_id);

grant select on public.post_feed, public.comment_feed, public.crew_feed, public.course_feed
  to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0005_ranking.sql
-- ══════════════════════════════════════════════════════════════════

-- 랭킹.
--
-- 지금 앱의 순위표는 코드에 박아 둔 가상의 러너 15명과 나를 섞은 것이다.
-- 그 화면이 처음부터 하려던 말은 "당신은 지금 몇 등입니다"인데, 상대가 가짜면
-- 그 말은 거짓이다. 이 파일이 그 자리에 진짜 사람을 넣는다.
--
-- ── 여기만 다른 규칙: 뷰가 RLS 를 지나간다 ──
--
-- 다른 곳에서는 뷰에 security_invoker 를 붙여 "읽는 사람 권한으로" 보게 했다.
-- 랭킹은 반대여야 한다. 남의 세션은 RLS 가 막는 게 맞고, 그렇지만 순위는
-- 남들과 비교해야 나온다. 그래서 여기 뷰는 소유자 권한으로 돌아 RLS 를 지나고,
-- 대신 **합계 말고는 아무것도 내보내지 않는다.** 원본 행 — 언제 어디를 뛰었는지 —
-- 은 여전히 본인만 본다.

-- ════════════════════════════════════════════════════════════════════
--  러너별 합계
-- ════════════════════════════════════════════════════════════════════

drop view if exists public.runner_stats cascade;
create view public.runner_stats as
  select
    p.id as user_id,
    p.display_name,
    p.avatar_id,
    -- 서버가 GPS 경로에서 직접 잰 값. 앱이 보낸 속도가 아니다.
    p.top_speed_kmh,
    coalesce(a.active_sec, 0)::bigint as active_sec,
    coalesce(a.km, 0)::double precision as km,
    coalesce(l.earned, 0)::numeric(20, 4) as sup
  from public.profiles p
  left join (
    select
      s.user_id,
      sum(s.duration_sec) as active_sec,
      sum(s.distance_meters) / 1000.0 as km
    from public.walk_sessions s
    -- 판정에서 떨어진 세션은 순위에 쓰지 않는다. 적립은 막아 놓고 순위는
    -- 올려 주면, 순위표는 막지 않은 쪽으로 뚫린다.
    where s.verdict <> 'VOID'
    group by s.user_id
  ) a on a.user_id = p.id
  left join (
    -- 누적 "적립"이다. 잔고가 아니다 — 쓴 사람이 순위에서 밀리면
    -- 상점은 아무도 안 쓰는 방이 된다.
    select user_id, sum(amount) filter (where amount > 0) as earned
      from public.sup_ledger
     group by user_id
  ) l on l.user_id = p.id;

comment on view public.runner_stats is
  '러너별 합계. 소유자 권한으로 돌아 RLS 를 지나므로 앱에는 직접 열어 주지 않는다 — 아래 함수로만 나간다.';

-- 앱에서 직접 읽지 못하게 한다. 이 뷰를 그대로 열면 전체 사용자 목록을
-- 통째로 받아 갈 수 있다. 순위에 필요한 건 상위 몇 명과 내 줄뿐이다.
revoke all on public.runner_stats from anon, authenticated;

-- ── 이름 약자 ──
-- 순위표의 동그라미 안에 들어갈 두 글자. "Maya C." → MC, "김러너" → 김러.
create or replace function public.monogram_of(p_name text)
returns text
language sql
immutable
as $$
  select case
    when coalesce(trim(p_name), '') = '' then '??'
    when array_length(regexp_split_to_array(trim(p_name), '\s+'), 1) >= 2 then
      upper(
        substr((regexp_split_to_array(trim(p_name), '\s+'))[1], 1, 1) ||
        substr((regexp_split_to_array(trim(p_name), '\s+'))[2], 1, 1)
      )
    else upper(substr(trim(p_name), 1, 2))
  end
$$;

-- ════════════════════════════════════════════════════════════════════
--  개인 순위
-- ════════════════════════════════════════════════════════════════════

drop function if exists public.leaderboard(text, int);

/*
 * 상위 몇 명과 **내 줄**을 함께 돌려준다.
 *
 * 내 줄을 끼워 주는 것이 핵심이다. 상위 20명만 주면 300등인 사람은 자기가
 * 어디 있는지 영영 모른다. 그러면 순위표는 남의 이야기가 된다.
 *
 * @param p_board TOP_SPEED(최고 속도) · LONGEST_TIME(누적 시간) · TOTAL_SUP(누적 적립)
 */
create or replace function public.leaderboard(
  p_board text default 'TOP_SPEED',
  p_limit int default 20
)
returns table (
  rank int,
  user_id uuid,
  name text,
  monogram text,
  top_speed_kmh double precision,
  active_sec bigint,
  sup numeric,
  is_me boolean
)
language sql
stable
security definer
set search_path = public
as $$
  with board as (
    select upper(coalesce(nullif(trim(p_board), ''), 'TOP_SPEED')) as kind
  ),
  ranked as (
    select
      s.*,
      rank() over (
        order by
          case (select kind from board)
            when 'LONGEST_TIME' then s.active_sec::numeric
            when 'TOTAL_SUP' then s.sup
            else s.top_speed_kmh::numeric
          end desc,
          -- 같은 값이면 이름순. 순서가 매번 흔들리면 순위표를 믿지 않게 된다.
          s.display_name,
          s.user_id
      )::int as rnk
    from public.runner_stats s
    -- 아직 한 번도 안 뛴 사람은 순위에 넣지 않는다. 0 으로 채운 줄이
    -- 수백 개면 순위표가 아니라 가입자 명단이다.
    where s.active_sec > 0 or s.sup > 0 or s.top_speed_kmh > 0
  )
  select
    r.rnk,
    r.user_id,
    r.display_name,
    public.monogram_of(r.display_name),
    r.top_speed_kmh,
    r.active_sec,
    r.sup,
    r.user_id = auth.uid()
  from ranked r
  where r.rnk <= greatest(coalesce(p_limit, 20), 1)
     or r.user_id = auth.uid()
  order by r.rnk
$$;

comment on function public.leaderboard is
  '상위 p_limit 명과 내 줄. 300등이어도 자기 자리가 보여야 순위표가 내 이야기가 된다.';

-- ════════════════════════════════════════════════════════════════════
--  종족 순위
--
--  개인 순위가 "나 vs 남"이라면 이건 "우리 vs 저쪽"이다. 내가 1등을 못 해도
--  우리 종족은 1등일 수 있다.
-- ════════════════════════════════════════════════════════════════════

drop function if exists public.faction_leaderboard();

create or replace function public.faction_leaderboard()
returns table (
  faction text,
  km double precision,
  my_km double precision,
  runners int
)
language sql
stable
security definer
set search_path = public
as $$
  select
    f.faction,
    coalesce(sum(s.distance_meters), 0) / 1000.0,
    coalesce(sum(s.distance_meters) filter (where s.user_id = auth.uid()), 0) / 1000.0,
    count(distinct s.user_id)::int
  from (values ('FIRE'), ('WATER'), ('LIGHTNING'), ('WIND')) as f(faction)
  left join public.walk_sessions s
    on s.faction = f.faction and s.verdict <> 'VOID'
  group by f.faction
  order by 2 desc, 1
$$;

comment on function public.faction_leaderboard is
  '종족별 누적 거리와 그중 내 몫. 신발을 고르는 일이 소속을 정하는 일이 된다.';

grant execute on function public.leaderboard(text, int) to authenticated;
grant execute on function public.faction_leaderboard() to authenticated;
grant execute on function public.monogram_of(text) to authenticated;

commit;

-- ════════════════════════════════════════════════════════════════════
--  끝났습니다. 아래로 확인할 수 있습니다.
-- ════════════════════════════════════════════════════════════════════
select table_name as "만들어진 표"
  from information_schema.tables
 where table_schema = 'public' and table_type = 'BASE TABLE'
 order by table_name;
