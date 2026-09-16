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
--  끝나면 왼쪽 Table Editor 에 표 4개가 보입니다:
--    profiles · daily_steps · walk_sessions · sup_ledger
--
--  이 파일은 supabase/migrations/ 의 파일들을 순서대로 이어 붙인 것입니다.
--  내용을 고칠 때는 그쪽을 고치고 scripts/build-setup-sql.py 로 다시 만드세요.
-- ════════════════════════════════════════════════════════════════════

begin;

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
