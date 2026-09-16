-- 스키마가 약속한 것을 실제로 지키는지 확인한다.
--
-- 이 파일은 Supabase 에 올리는 것이 아니다. 로컬 Postgres 에서 돌린다:
--
--     supabase/tests/run.sh
--
-- 맞지 않는 것이 하나라도 있으면 그 자리에서 멈춘다. "아마 될 것이다"로
-- 넘어간 규칙은 실제로는 안 되는 경우가 많고, 돈이 걸린 표에서는 그 차이가
-- 나중에 원장으로 나타난다.

\set ON_ERROR_STOP on
set client_min_messages = notice;

create or replace function pg_temp.ok(cond boolean, label text) returns void
language plpgsql as $$
begin
  if cond then
    raise notice '  OK   %', label;
  else
    raise exception 'FAIL  %', label;
  end if;
end $$;

-- 로그인 흉내. auth.uid() 가 이 값을 읽는다.
-- 함수가 아니라 프로시저인 것은 call 이 결과 행을 찍지 않아서다 —
-- 검사 결과 사이에 빈 표가 끼면 읽기가 나빠진다.
create or replace procedure pg_temp.login(p_user uuid)
language plpgsql as $$
begin
  perform set_config('request.jwt.claim.sub', coalesce(p_user::text, ''), false);
end $$;

-- 실패해야 하는 일. 성공해 버리면 그게 사고다.
create or replace procedure pg_temp.must_fail(p_sql text, label text)
language plpgsql as $$
begin
  begin
    execute p_sql;
  exception when others then
    raise notice '  OK   % (%)', label, replace(sqlerrm, E'\n', ' ');
    return;
  end;
  raise exception 'FAIL  % — 막혔어야 하는데 통과했다', label;
end $$;

-- 준비물 하나 꺼내기.
-- CALL 의 인자에는 서브쿼리를 넣을 수 없어서 함수로 감싼다.
create or replace function pg_temp.fx(p_key text) returns text
language plpgsql as $$
declare v_out text;
begin
  select v into v_out from fix where k = p_key;
  return v_out;
end $$;

-- 일정한 속도로 달린 경로를 만든다.
-- @param p_dps  1초에 움직이는 위도(도). 0.00003 이면 대략 시속 12km.
create or replace function pg_temp.track(p_start timestamptz, p_secs int, p_dps numeric)
returns text language sql as $$
  select string_agg(
    format('%s,%s,%s',
      (37.5 + i * p_dps)::numeric(12, 6),
      127.000000,
      (extract(epoch from p_start) * 1000)::bigint + i * 1000),
    ';' order by i)
  from generate_series(0, p_secs) i
$$;

create temp table fix (k text primary key, v text);
-- 앱 권한(authenticated)으로 바꿔 검사하는 동안에도 준비물은 읽어야 한다.
-- 앱 권한(authenticated)으로 바꿔 검사하는 동안에도 준비물은 읽어야 한다.
grant all on fix to authenticated;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 준비 ─────────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

insert into auth.users (id, email, raw_user_meta_data) values
  ('11111111-1111-1111-1111-111111111111', 'a@test', '{"full_name":"Ara Kim"}'),
  ('22222222-2222-2222-2222-222222222222', 'b@test', '{"full_name":"Bo Lee"}'),
  ('33333333-3333-3333-3333-333333333333', 'c@test', '{"full_name":"Cha Park"}');

do $$
begin
  perform pg_temp.ok(
    (select count(*) from public.profiles) = 3,
    '가입하면 프로필이 자동으로 생긴다');
  perform pg_temp.ok(
    (select display_name from public.profiles
      where id = '11111111-1111-1111-1111-111111111111') = 'Ara Kim',
    '구글이 준 이름이 프로필에 들어간다');
end $$;

-- 모든 세션 시각을 "어제 10시"에서 잰다.
--
-- now() 에 매달면 자정 무렵에 돌릴 때 세션이 이틀에 걸쳐, 하루 상한 검사가
-- 이유 없이 무너진다. 테스트가 시계에 따라 결과가 달라지면 그 테스트는
-- 못 믿는다.
insert into fix (k, v) values
  ('base', (date_trunc('day', now() - interval '1 day') + interval '10 hours')::text);

insert into fix (k, v)
  select 'run_start', (pg_temp.fx('base')::timestamptz)::text;
insert into fix (k, v)
  select 'void_start', (pg_temp.fx('base')::timestamptz + interval '2 hours')::text;
insert into fix (k, v)
  select 'car_start', (pg_temp.fx('base')::timestamptz + interval '3 hours')::text;
insert into fix (k, v)
  select 'cap_start', (pg_temp.fx('base')::timestamptz + interval '4 hours')::text;
insert into fix (k, v)
  select 'last_start', (pg_temp.fx('base')::timestamptz + interval '6 hours')::text;

insert into fix (k, v)
  select 'track_ok', pg_temp.track(pg_temp.fx('run_start')::timestamptz, 600, 0.00003);
insert into fix (k, v)
  select 'track_car', pg_temp.track(pg_temp.fx('car_start')::timestamptz, 600, 0.002);

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 경로에서 속도 읽기 ───────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

do $$
declare v_kmh double precision; v_glitch double precision;
begin
  select top_speed_kmh, glitch_ratio into v_kmh, v_glitch
    from economy.track_speed_stats(pg_temp.fx('track_ok'));
  perform pg_temp.ok(v_kmh between 11.5 and 12.5,
    format('시속 12km 로 만든 경로에서 %s km/h 를 읽는다', round(v_kmh::numeric, 2)));
  perform pg_temp.ok(v_glitch = 0, '정상 경로에는 튄 구간이 없다');

  select top_speed_kmh, glitch_ratio into v_kmh, v_glitch
    from economy.track_speed_stats(pg_temp.fx('track_car'));
  perform pg_temp.ok(v_glitch > 0.5, '차로 이동한 경로는 구간 대부분이 사람 속도를 넘는다');

  select top_speed_kmh into v_kmh from economy.track_speed_stats('');
  perform pg_temp.ok(v_kmh = 0, '경로가 없으면 속도는 0 이다');

  select top_speed_kmh into v_kmh from economy.track_speed_stats('깨진,값;37.5,127.0;,,');
  perform pg_temp.ok(v_kmh = 0, '깨진 경로에도 죽지 않는다');
end $$;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 적립 ─────────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

set role authenticated;
call pg_temp.login('11111111-1111-1111-1111-111111111111');

do $$
declare r record; v_start timestamptz := pg_temp.fx('run_start')::timestamptz;
begin
  select * into r from public.record_session(
    v_start, v_start + interval '601 seconds', 2000, 601,
    pg_temp.fx('track_ok'), 1200, 2, 'FIRE');

  -- 2000보 × 0.01 × 파티2(1.1) × 부스트1200bps(1.12) = 24.64
  perform pg_temp.ok(r.verdict = 'CLEAN', '정상 세션은 CLEAN');
  perform pg_temp.ok(r.points_awarded = 24.64,
    format('적립액을 서버가 계산한다 (%s SUP)', r.points_awarded));
  perform pg_temp.ok(r.balance = 24.64, '잔고가 적립만큼 늘었다');

  perform pg_temp.ok(
    (select faction from public.walk_sessions where id = r.session_id) = 'FIRE',
    '정산 시점 종족이 함께 남는다');
  perform pg_temp.ok(
    (select top_speed_kmh from public.walk_sessions where id = r.session_id) between 11.5 and 12.5,
    '세션의 최고 속도는 경로에서 나온 값이다');
  perform pg_temp.ok(
    (select top_speed_kmh from public.profiles
      where id = '11111111-1111-1111-1111-111111111111') between 11.5 and 12.5,
    '프로필의 최고 기록이 갱신된다');
end $$;

do $$
declare r record; v_start timestamptz := pg_temp.fx('run_start')::timestamptz;
begin
  -- 지하철에서 응답을 못 받고 앱이 다시 보낸 경우
  select * into r from public.record_session(
    v_start, v_start + interval '601 seconds', 2000, 601,
    pg_temp.fx('track_ok'), 1200, 2, 'FIRE');
  perform pg_temp.ok(r.points_awarded = 24.64, '같은 세션을 다시 보내도 결과가 같다');
  perform pg_temp.ok(
    (select coalesce(sum(amount), 0) from public.sup_ledger) = 24.64,
    '다시 보내도 원장에 두 번 쌓이지 않는다');
end $$;

do $$
declare r record; v_start timestamptz := pg_temp.fx('void_start')::timestamptz;
begin
  -- 1분에 500보 — 사람의 다리가 아니다
  select * into r from public.record_session(
    v_start, v_start + interval '120 seconds', 1000, 120, '', 0, 1, 'FIRE');
  perform pg_temp.ok(r.verdict = 'VOID', '케이던스가 사람 범위를 벗어나면 VOID');
  perform pg_temp.ok(r.points_awarded = 0, 'VOID 세션은 적립이 없다');
end $$;

do $$
declare r record; v_start timestamptz := pg_temp.fx('car_start')::timestamptz;
begin
  -- 차를 타고 이동한 경로. 케이던스는 정상 범위로 맞춰 둔다 —
  -- 속도 판정만으로 걸리는지 보려는 것이다.
  select * into r from public.record_session(
    v_start, v_start + interval '601 seconds', 1200, 601,
    pg_temp.fx('track_car'), 0, 1, 'WIND');
  perform pg_temp.ok(r.verdict = 'VOID', '차로 이동한 경로는 VOID');
  perform pg_temp.ok(r.points_awarded = 0, '타고 간 거리는 적립되지 않는다');
  perform pg_temp.ok(
    (select top_speed_kmh from public.profiles
      where id = '11111111-1111-1111-1111-111111111111') between 11.5 and 12.5,
    'VOID 세션은 최고 속도 기록도 남기지 않는다');
end $$;

do $$
declare r record; v_start timestamptz := pg_temp.fx('cap_start')::timestamptz;
begin
  -- 하루 상한 48,000보. 이미 2,000보를 적립했으므로 46,000보만 인정되어야 한다.
  select * into r from public.record_session(
    v_start, v_start + interval '20000 seconds', 50000, 20000, '', 0, 1, '');
  perform pg_temp.ok(r.verdict = 'FLAGGED', '하루 상한을 넘으면 FLAGGED');
  perform pg_temp.ok(
    (select rewarded_steps from public.walk_sessions where id = r.session_id) = 46000,
    '상한까지만 인정된다 (46,000보)');
end $$;

do $$
declare r record; v_start timestamptz := pg_temp.fx('last_start')::timestamptz;
begin
  select * into r from public.record_session(
    v_start, v_start + interval '1000 seconds', 3000, 1000, '', 0, 1, '');
  perform pg_temp.ok(
    (select rewarded_steps from public.walk_sessions where id = r.session_id) = 0,
    '상한을 채운 뒤에는 0보만 인정된다');
end $$;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 소비 ─────────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

do $$
declare v_left numeric;
begin
  select public.spend_sup('SPEND_BOOST', 10, '부스트') into v_left;
  perform pg_temp.ok(v_left = (24.64 + 460.00) - 10, format('차감 뒤 잔고 %s', v_left));
end $$;

call pg_temp.must_fail(
  $q$ select public.spend_sup('SPEND_MINT', 999999, '민팅') $q$,
  '잔고보다 많이 쓸 수 없다');
call pg_temp.must_fail(
  $q$ select public.spend_sup('EARN_WALK', 10, '적립인 척') $q$,
  '소비 함수로 적립할 수 없다');

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 원장·세션에 직접 쓰기 ────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

call pg_temp.must_fail(
  $q$ insert into public.sup_ledger (user_id, kind, amount)
      values ('11111111-1111-1111-1111-111111111111', 'EARN_WALK', 100000) $q$,
  '원장에 직접 적립할 수 없다');
call pg_temp.must_fail(
  $q$ update public.sup_ledger set amount = 100000 $q$,
  '원장 금액을 고칠 수 없다');
call pg_temp.must_fail(
  $q$ delete from public.sup_ledger $q$,
  '원장을 지울 수 없다');
call pg_temp.must_fail(
  $q$ insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps)
      values ('11111111-1111-1111-1111-111111111111', now(), now(), 0, 99999) $q$,
  '세션을 직접 넣을 수 없다');

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 남의 것 ──────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

call pg_temp.login('22222222-2222-2222-2222-222222222222');

do $$
begin
  perform pg_temp.ok((select count(*) from public.sup_ledger) = 0, '남의 원장은 보이지 않는다');
  perform pg_temp.ok((select count(*) from public.walk_sessions) = 0, '남의 세션은 보이지 않는다');
  perform pg_temp.ok((select count(*) from public.profiles) = 3, '프로필은 서로 보인다');

  update public.profiles set display_name = '가로채기'
   where id = '11111111-1111-1111-1111-111111111111';
  perform pg_temp.ok(
    (select display_name from public.profiles
      where id = '11111111-1111-1111-1111-111111111111') = 'Ara Kim',
    '남의 프로필은 고쳐지지 않는다');
end $$;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 커뮤니티 ─────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

call pg_temp.login('11111111-1111-1111-1111-111111111111');

do $$
declare v_crew uuid;
begin
  insert into public.crews (owner_id, name, monogram, tagline, area)
  values ('11111111-1111-1111-1111-111111111111', '한강 러너스', 'HR', '매주 토요일', '서울 마포')
  returning id into v_crew;
  insert into fix (k, v) values ('crew', v_crew::text);

  perform pg_temp.ok(
    (select count(*) from public.crew_members
      where crew_id = v_crew and user_id = '11111111-1111-1111-1111-111111111111'
        and role = 'OWNER') = 1,
    '크루를 만들면 만든 사람이 바로 주인 멤버가 된다');
  perform pg_temp.ok(
    (select member_count from public.crew_feed where id = v_crew) = 1,
    '크루 목록에 인원이 세어진다');
end $$;

-- 정책은 예외를 던지지 않고 0행으로 막는다. 그래서 must_fail 이 아니라
-- 지운 뒤에 남아 있는지로 확인한다.
do $$
begin
  delete from public.crew_members
   where crew_id = pg_temp.fx('crew')::uuid
     and user_id = '11111111-1111-1111-1111-111111111111';
  perform pg_temp.ok(
    (select count(*) from public.crew_members
      where crew_id = pg_temp.fx('crew')::uuid) = 1,
    '주인은 크루에서 나갈 수 없다 (0행 삭제)');
end $$;

-- 크루 전용 글과 전체 글
do $$
declare v_post bigint; v_crew uuid := pg_temp.fx('crew')::uuid;
begin
  insert into public.posts (author_id, category, title, body)
  values ('11111111-1111-1111-1111-111111111111', 'FREE', '오늘 날씨 좋네요', '한강 추천')
  returning id into v_post;
  insert into fix (k, v) values ('post_open', v_post::text);

  insert into public.posts (author_id, category, crew_id, title, body)
  values ('11111111-1111-1111-1111-111111111111', 'TIP', v_crew, '크루만 보는 글', '내일 6시')
  returning id into v_post;
  insert into fix (k, v) values ('post_crew', v_post::text);

  insert into public.posts (author_id, category, title, place, distance_km, meet_at, capacity)
  values ('11111111-1111-1111-1111-111111111111', 'FLASH', '오늘 저녁 7시 번개',
          '여의도 한강공원', 5, now() + interval '3 hours', 2)
  returning id into v_post;
  insert into fix (k, v) values ('post_flash', v_post::text);
end $$;

call pg_temp.login('22222222-2222-2222-2222-222222222222');

do $$
begin
  perform pg_temp.ok(
    (select count(*) from public.post_feed
      where id = pg_temp.fx('post_open')::bigint) = 1,
    '전체 게시판 글은 누구나 본다');
  perform pg_temp.ok(
    (select count(*) from public.post_feed
      where id = pg_temp.fx('post_crew')::bigint) = 0,
    '크루 글은 멤버가 아니면 안 보인다');
end $$;

call pg_temp.must_fail(
  format($q$ insert into public.comments (post_id, parent_id, author_id, body)
             values (%s, null, '22222222-2222-2222-2222-222222222222', '안 보이는데 댓글') $q$,
         pg_temp.fx('post_crew')),
  '안 보이는 글에는 댓글을 못 단다');

call pg_temp.must_fail(
  format($q$ insert into public.posts (author_id, category, crew_id, title)
             values ('22222222-2222-2222-2222-222222222222', 'FREE', '%s', '남의 크루에 글쓰기') $q$,
         pg_temp.fx('crew')),
  '안 들어간 크루 게시판에는 글을 못 쓴다');

call pg_temp.must_fail(
  $q$ insert into public.posts (author_id, category, title)
      values ('11111111-1111-1111-1111-111111111111', 'FREE', '남의 이름으로') $q$,
  '남의 이름으로 글을 쓸 수 없다');

-- 가입하면 보인다
do $$
begin
  insert into public.crew_members (crew_id, user_id)
  values (pg_temp.fx('crew')::uuid, '22222222-2222-2222-2222-222222222222');
  perform pg_temp.ok(
    (select count(*) from public.post_feed
      where id = pg_temp.fx('post_crew')::bigint) = 1,
    '크루에 들어가면 크루 글이 보인다');
end $$;

call pg_temp.must_fail(
  format($q$ insert into public.crew_members (crew_id, user_id, role)
             values ('%s', '33333333-3333-3333-3333-333333333333', 'OWNER') $q$,
         pg_temp.fx('crew')),
  '남을 대신 가입시킬 수 없다');

-- 좋아요와 댓글
do $$
declare v_post bigint := pg_temp.fx('post_open')::bigint;
begin
  insert into public.post_likes (post_id, user_id)
  values (v_post, '22222222-2222-2222-2222-222222222222');
  insert into public.comments (post_id, author_id, body)
  values (v_post, '22222222-2222-2222-2222-222222222222', '저도 갈래요');

  perform pg_temp.ok(
    (select likes from public.post_feed where id = v_post) = 1, '좋아요가 세어진다');
  perform pg_temp.ok(
    (select liked from public.post_feed where id = v_post), '내가 누른 좋아요가 표시된다');
  perform pg_temp.ok(
    (select comment_count from public.post_feed where id = v_post) = 1, '댓글 수가 세어진다');
  perform pg_temp.ok(
    not (select mine from public.post_feed where id = v_post), '남의 글은 mine 이 아니다');
  perform pg_temp.ok(
    (select author from public.post_feed where id = v_post) = 'Ara Kim',
    '작성자 이름이 프로필에서 온다');
end $$;

-- 번개러닝 정원
do $$
declare v_post bigint := pg_temp.fx('post_flash')::bigint; v_n int;
begin
  select public.join_flash(v_post) into v_n;
  perform pg_temp.ok(v_n = 1, '번개에 참가하면 인원이 센다');
  select public.join_flash(v_post) into v_n;
  perform pg_temp.ok(v_n = 1, '두 번 눌러도 한 명이다');
end $$;

call pg_temp.must_fail(
  format($q$ insert into public.flash_participants (post_id, user_id)
             values (%s, '22222222-2222-2222-2222-222222222222') $q$,
         pg_temp.fx('post_flash')),
  '참가자 표에 직접 넣을 수 없다');

call pg_temp.login('33333333-3333-3333-3333-333333333333');
do $$
declare v_n int;
begin
  select public.join_flash(pg_temp.fx('post_flash')::bigint) into v_n;
  perform pg_temp.ok(v_n = 2, '정원 2명 중 둘째가 들어간다');
end $$;

call pg_temp.login('11111111-1111-1111-1111-111111111111');
call pg_temp.must_fail(
  format($q$ select public.join_flash(%s) $q$, pg_temp.fx('post_flash')),
  '정원이 차면 더 들어갈 수 없다');

-- 차단
call pg_temp.login('33333333-3333-3333-3333-333333333333');
do $$
begin
  perform pg_temp.ok(
    (select count(*) from public.post_feed
      where id = pg_temp.fx('post_open')::bigint) = 1,
    '차단 전에는 글이 보인다');

  insert into public.user_blocks (blocker_id, blocked_id)
  values ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111');

  perform pg_temp.ok(
    (select count(*) from public.post_feed
      where id = pg_temp.fx('post_open')::bigint) = 0,
    '차단하면 그 사람 글이 내 화면에서 사라진다');
end $$;

call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$
begin
  perform pg_temp.ok(
    (select count(*) from public.post_feed
      where id = pg_temp.fx('post_open')::bigint) = 1,
    '남이 차단해도 내 화면은 그대로다');
  perform pg_temp.ok(
    (select count(*) from public.user_blocks) = 0, '남의 차단 목록은 보이지 않는다');
end $$;

-- 신고
do $$
begin
  insert into public.content_reports (reporter_id, target_type, target_id, reason, note)
  values ('22222222-2222-2222-2222-222222222222', 'POST',
          pg_temp.fx('post_open'), 'SPAM', '광고입니다');
  perform pg_temp.ok((select count(*) from public.content_reports) = 1, '신고를 넣을 수 있다');
end $$;

call pg_temp.must_fail(
  $q$ update public.content_reports set status = 'DISMISSED' $q$,
  '신고 처리 상태를 앱이 바꿀 수 없다');

-- 코스
call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
begin
  insert into public.courses (owner_id, name, area, distance_km, track, shared)
  values ('11111111-1111-1111-1111-111111111111', '한강 5km', '서울 마포', 5.0,
          '37.5,127.0;37.51,127.0', true);
  insert into public.courses (owner_id, name, area, distance_km, shared)
  values ('11111111-1111-1111-1111-111111111111', '혼자 보는 코스', '서울', 3.0, false);
  perform pg_temp.ok((select count(*) from public.course_feed) = 2, '내 코스는 다 보인다');
end $$;

call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$
begin
  perform pg_temp.ok((select count(*) from public.course_feed) = 1,
    '공유하지 않은 코스는 남에게 안 보인다');
end $$;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 랭킹 ─────────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

call pg_temp.must_fail(
  $q$ select count(*) from public.runner_stats $q$,
  '러너 합계 뷰를 앱이 직접 읽을 수 없다');

call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
declare r record;
begin
  select * into r from public.leaderboard('TOP_SPEED', 20) where is_me;
  perform pg_temp.ok(r.rank = 1, '뛴 사람이 나뿐이면 1등이다');
  perform pg_temp.ok(r.monogram = 'AK', '이름 약자가 나온다 (Ara Kim → AK)');
  perform pg_temp.ok(r.top_speed_kmh between 11.5 and 12.5, '순위표의 속도는 경로에서 나온 값이다');

  select * into r from public.leaderboard('TOTAL_SUP', 20) where is_me;
  -- 적립은 24.64 + 460.00, 쓴 10 은 순위에서 빼지 않는다
  perform pg_temp.ok(r.sup = 484.64,
    format('누적 적립으로 줄을 세운다 (쓴 돈은 빼지 않는다, %s)', r.sup));

  perform pg_temp.ok(
    (select count(*) from public.leaderboard('LONGEST_TIME', 20)) >= 1,
    '누적 시간 순위도 나온다');
end $$;

do $$
declare r record;
begin
  select * into r from public.faction_leaderboard() where faction = 'FIRE';
  perform pg_temp.ok(r.km > 0, '종족에 거리가 쌓인다');
  perform pg_temp.ok(r.my_km = r.km, '내 몫이 따로 나온다');

  select * into r from public.faction_leaderboard() where faction = 'WIND';
  perform pg_temp.ok(r.km = 0, 'VOID 세션은 종족에도 쌓이지 않는다');

  perform pg_temp.ok((select count(*) from public.faction_leaderboard()) = 4,
    '아무도 안 뛴 종족도 줄은 나온다');
end $$;

-- 300등도 자기 줄이 보여야 한다
call pg_temp.login('33333333-3333-3333-3333-333333333333');
do $$
declare v_start timestamptz := pg_temp.fx('base')::timestamptz + interval '8 hours';
begin
  perform public.record_session(v_start, v_start + interval '300 seconds', 300, 300, '', 0, 1, 'WATER');
  perform pg_temp.ok(
    (select count(*) from public.leaderboard('TOP_SPEED', 1) where is_me) = 1,
    '상위 1명만 받아도 내 줄은 함께 온다');
end $$;

reset role;

\echo ''
\echo '════════════════════════════════════════════════════════════════'
\echo ' 전부 통과했습니다.'
\echo '════════════════════════════════════════════════════════════════'
