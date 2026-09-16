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
