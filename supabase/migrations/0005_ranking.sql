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
  is_me boolean,
  -- 순위에 오른 전체 인원. "300명 중 47등"의 300 이다. 상위 몇 명만 받으면
  -- 앱은 이 수를 알 방법이 없어서, 받은 줄 수를 전체인 양 보여주게 된다.
  total int
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
    r.user_id = auth.uid(),
    r.total
  from ranked r
  where r.rnk <= greatest(coalesce(p_limit, 20), 1)
     or r.user_id = auth.uid()
  order by r.rnk
$$;

-- 인자 목록을 적어 둔다. 뒤에 나오는 0006 이 인자를 하나 더한 같은 이름의
-- 함수를 만들기 때문에, 이 파일을 다시 돌릴 때 이름만으로는 어느 쪽인지
-- 가릴 수 없다.
comment on function public.leaderboard(text, int) is
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

comment on function public.faction_leaderboard() is
  '종족별 누적 거리와 그중 내 몫. 신발을 고르는 일이 소속을 정하는 일이 된다.';

grant execute on function public.leaderboard(text, int) to authenticated;
grant execute on function public.faction_leaderboard() to authenticated;
grant execute on function public.monogram_of(text) to authenticated;
