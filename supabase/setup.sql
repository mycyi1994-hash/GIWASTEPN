-- ════════════════════════════════════════════════════════════════════
--  StepUp 서버 스키마 — 전체 설치
--
--  Supabase 대시보드 → SQL Editor 에 이 파일 전체를 붙여넣고 Run 하세요.
--  한 번에 다 만들어집니다.
--
--  끝나면 왼쪽 Table Editor 에 표 4개가 보입니다:
--    profiles · daily_steps · walk_sessions · sup_ledger
--
--  이 파일은 supabase/migrations/ 의 파일들을 순서대로 이어 붙인 것입니다.
--  내용을 고칠 때는 그쪽을 고치고 scripts/build-setup-sql.py 로 다시 만드세요.
-- ════════════════════════════════════════════════════════════════════

begin;

-- ══════════════════════════════════════════════════════════════════
-- 0001_profiles.sql
-- ══════════════════════════════════════════════════════════════════

-- 계정과 프로필.
--
-- Supabase 의 auth.users 는 건드리지 않는다. 그 표는 인증이 소유하고, 우리는
-- 곁에 프로필 표를 두어 1:1로 붙인다. 이렇게 두면 인증 방식이 바뀌어도
-- (구글에서 애플로, 또는 익명 계정 추가) 프로필은 그대로 산다.

create table public.profiles (
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
create policy profiles_select_all
  on public.profiles for select
  using (true);

-- 고치는 것은 본인만.
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
create table public.daily_steps (
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
create table public.walk_sessions (
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
create unique index walk_sessions_user_started_unique
  on public.walk_sessions (user_id, started_at);

create index walk_sessions_user_recent
  on public.walk_sessions (user_id, started_at desc);

-- ── 권한 ───────────────────────────────────────────────────────────
alter table public.daily_steps enable row level security;
alter table public.walk_sessions enable row level security;

-- 걸음은 본인 것만 읽고 쓴다.
create policy daily_steps_own
  on public.daily_steps for all
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

-- 세션은 본인 것만 읽는다.
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

create table public.sup_ledger (
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

create index sup_ledger_user_recent on public.sup_ledger (user_id, occurred_at desc);

-- 한 세션이 두 번 적립되는 것을 막는다.
create unique index sup_ledger_session_once
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
  p_party_size int default 1
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

  -- ── 판정 ──
  --
  -- 사람의 이동으로 보기 어려운 세션은 적립하지 않는다. 여기서는 케이던스만
  -- 본다 — GPS 경로 검사는 좌표를 하나씩 훑어야 해서 Edge Function 쪽이 맞고,
  -- 케이던스는 나눗셈 한 번이라 여기서 끝난다.
  if v_elapsed >= 60 and p_steps::numeric * 60 / v_elapsed > 240 then
    v_verdict := 'VOID';
    v_reason := '케이던스가 사람 범위를 벗어납니다';
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
    verdict, verdict_reason, points_awarded, rewarded_steps
  )
  values (
    v_user, p_started_at, p_ended_at, v_elapsed, p_steps,
    p_steps * 0.762, p_steps * 0.04, coalesce(p_track, ''), p_boost_bps, p_party_size,
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

  -- 랭킹 재료 갱신
  update public.profiles
     set lifetime_km = lifetime_km + (p_steps * 0.762 / 1000)
   where id = v_user;

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

grant execute on function public.record_session(timestamptz, timestamptz, int, int, text, int, int)
  to authenticated;
grant execute on function public.spend_sup(text, numeric, text) to authenticated;

commit;

-- ════════════════════════════════════════════════════════════════════
--  끝났습니다. 아래로 확인할 수 있습니다.
-- ════════════════════════════════════════════════════════════════════
select table_name as "만들어진 표"
  from information_schema.tables
 where table_schema = 'public' and table_type = 'BASE TABLE'
 order by table_name;
