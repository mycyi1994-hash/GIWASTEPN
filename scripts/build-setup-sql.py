#!/usr/bin/env python3
"""supabase/migrations/*.sql 을 이어 붙여 supabase/setup.sql 을 만든다.

대시보드 SQL Editor 에 한 번에 붙여넣을 수 있게 하나로 합치는 것이 목적이다.
파일을 세 번 나눠 복사하는 것은 순서를 틀리기 쉽고, 틀리면 중간에서 실패한다.
"""
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
MIG = ROOT / "supabase" / "migrations"
OUT = ROOT / "supabase" / "setup.sql"

HEADER = """-- ════════════════════════════════════════════════════════════════════
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

"""

FOOTER = """
commit;

-- ════════════════════════════════════════════════════════════════════
--  끝났습니다. 아래로 확인할 수 있습니다.
-- ════════════════════════════════════════════════════════════════════
select table_name as "만들어진 표"
  from information_schema.tables
 where table_schema = 'public' and table_type = 'BASE TABLE'
 order by table_name;
"""


def main() -> int:
    files = sorted(MIG.glob("*.sql"))
    if not files:
        print("마이그레이션 파일이 없습니다", file=sys.stderr)
        return 1

    chunks = []
    for path in files:
        banner = "-- " + "═" * 66
        chunks.append(
            f"{banner}\n-- {path.name}\n{banner}\n\n{path.read_text(encoding='utf-8').rstrip()}\n"
        )

    OUT.write_text(HEADER + "\n".join(chunks) + FOOTER, encoding="utf-8")
    print(f"{OUT.relative_to(ROOT)} — {len(files)}개 파일, {len(OUT.read_text(encoding='utf-8').splitlines())}줄")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
