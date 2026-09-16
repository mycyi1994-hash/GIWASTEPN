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
