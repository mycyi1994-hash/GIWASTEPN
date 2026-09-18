-- 랭킹의 기간.
--
-- 전체기간만 있으면 순위표는 일찍 시작한 사람의 명단이 된다. 어제 가입한
-- 사람이 아무리 달려도 3년 치 누적을 따라잡을 수 없고, 따라잡을 수 없는
-- 순위표는 두 번 보지 않는다. 오늘·이번 주·이번 달은 누구에게나 0부터다.
--
-- 기간을 "월요일 0시"처럼 달력으로 끊지 않고 최근 N일로 잡았다. 달력으로
-- 끊으면 일요일 밤에 올린 기록이 몇 시간 만에 순위에서 사라지고, 사용자는
-- 기록이 지워졌다고 생각한다. 앱의 RankPeriod.sinceMillis 와 같은 규칙이다.

-- ── 기간의 시작 ────────────────────────────────────────────────────
--
-- now() 를 쓰므로 immutable 이 아니라 stable 이다. 한 질의 안에서는 같은
-- 값이고, 그것이면 충분하다.
create or replace function public.rank_period_start(p_period text default 'ALL')
returns timestamptz
language sql
stable
as $$
  select case upper(coalesce(nullif(trim(p_period), ''), 'ALL'))
    when 'DAY' then now() - interval '1 day'
    when 'WEEK' then now() - interval '7 days'
    when 'MONTH' then now() - interval '30 days'
    -- 전체기간. 어떤 시각과 비교해도 참이 되는 값이라 조건을 따로 두지 않아도 된다.
    else '-infinity'::timestamptz
  end
$$;

comment on function public.rank_period_start(text) is
  '랭킹 기간(DAY·WEEK·MONTH·ALL)의 시작 시각. 달력이 아니라 최근 N일로 끊는다.';

-- ════════════════════════════════════════════════════════════════════
--  개인 순위 — 기간을 받는다
-- ════════════════════════════════════════════════════════════════════

-- 기간을 세는 순위는 runner_stats(전체기간 합계)로는 만들 수 없다. 창을
-- 받아 그 안의 세션만 다시 모은다. 전체기간이면 창이 '-infinity' 라서
-- 예전과 같은 답이 나온다.
--
-- 인자가 하나 늘었으므로 예전 서명을 지운다. 그냥 두면 2개 인자로 부르는
-- 쪽은 기간을 모르는 옛 함수로 계속 간다.
drop function if exists public.leaderboard(text, int);

create or replace function public.leaderboard(
  p_board text default 'TOP_SPEED',
  p_limit int default 20,
  p_period text default 'ALL'
)
returns table (
  rank int,
  user_id uuid,
  name text,
  monogram text,
  top_speed_kmh double precision,
  active_sec bigint,
  sup numeric,
  is_me boolean,
  total int
)
language sql
stable
security definer
set search_path = public
as $$
  with win as (
    select public.rank_period_start(p_period) as since
  ),
  board as (
    select upper(coalesce(nullif(trim(p_board), ''), 'TOP_SPEED')) as kind
  ),
  stats as (
    select
      p.id as user_id,
      p.display_name,
      -- 창 안의 세션 중 가장 빨랐던 구간. 전체기간일 때만 프로필에 적힌
      -- 역대 최고와 견준다 — 세션 표가 생기기 전에 세워진 기록도 있다.
      greatest(
        coalesce(a.top_speed_kmh, 0),
        case
          when (select since from win) = '-infinity'::timestamptz then p.top_speed_kmh
          else 0
        end
      )::double precision as top_speed_kmh,
      coalesce(a.active_sec, 0)::bigint as active_sec,
      coalesce(l.earned, 0)::numeric(20, 4) as sup
    from public.profiles p
    left join (
      select
        s.user_id,
        max(s.top_speed_kmh) as top_speed_kmh,
        sum(s.duration_sec) as active_sec
      from public.walk_sessions s
      -- 판정에서 떨어진 세션은 순위에 쓰지 않는다. 적립은 막아 놓고 순위는
      -- 올려 주면, 순위표는 막지 않은 쪽으로 뚫린다.
      where s.verdict <> 'VOID'
        and s.started_at >= (select since from win)
      group by s.user_id
    ) a on a.user_id = p.id
    left join (
      -- 누적 "적립"이다. 잔고가 아니다 — 쓴 사람이 순위에서 밀리면
      -- 상점은 아무도 안 쓰는 방이 된다.
      select user_id, sum(amount) filter (where amount > 0) as earned
        from public.sup_ledger
       where occurred_at >= (select since from win)
       group by user_id
    ) l on l.user_id = p.id
  ),
  ranked as (
    select
      s.*,
      count(*) over ()::int as total,
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
    from stats s
    -- 이 기간에 아무것도 안 한 사람은 순위에 넣지 않는다. 0 으로 채운 줄이
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
    r.user_id = auth.uid(),
    r.total
  from ranked r
  where r.rnk <= greatest(coalesce(p_limit, 20), 1)
     or r.user_id = auth.uid()
  order by r.rnk
$$;

comment on function public.leaderboard(text, int, text) is
  '상위 p_limit 명과 내 줄을, p_period(DAY·WEEK·MONTH·ALL) 기간으로. 300등이어도 자기 자리가 보여야 순위표가 내 이야기가 된다.';

-- ════════════════════════════════════════════════════════════════════
--  종족 순위 — 기간을 받는다
-- ════════════════════════════════════════════════════════════════════

drop function if exists public.faction_leaderboard();

create or replace function public.faction_leaderboard(p_period text default 'ALL')
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
    on s.faction = f.faction
   and s.verdict <> 'VOID'
   and s.started_at >= public.rank_period_start(p_period)
  group by f.faction
  order by 2 desc, 1
$$;

comment on function public.faction_leaderboard(text) is
  '종족별 누적 거리와 그중 내 몫을, p_period 기간으로. 신발을 고르는 일이 소속을 정하는 일이 된다.';

grant execute on function public.rank_period_start(text) to authenticated;
grant execute on function public.leaderboard(text, int, text) to authenticated;
grant execute on function public.faction_leaderboard(text) to authenticated;
