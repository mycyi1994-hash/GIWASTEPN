#!/usr/bin/env bash
# 로컬 Postgres 에서 스키마 검사를 돌린다.
#
# Supabase 는 건드리지 않는다. 깨끗한 데이터베이스를 새로 만들고, setup.sql 을
# 그대로 올린 뒤, 앱과 같은 권한(authenticated)으로 규칙을 하나씩 두드려 본다.
#
#   supabase/tests/run.sh
#
# 필요한 것: postgresql 16 (initdb/pg_ctl/psql). 서버는 이 스크립트가 띄운다.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PGBIN="${PGBIN:-/usr/lib/postgresql/16/bin}"
RUNDIR="${RUNDIR:-/var/tmp/stepup-pgtest}"
PORT="${PGPORT:-55432}"
DB=stepup_schema_test

export PATH="$PGBIN:$PATH"

# Postgres 는 root 로 돌지 않는다. root 라면 postgres 사용자로 다시 부른다.
if [ "$(id -u)" = "0" ]; then
  mkdir -p "$RUNDIR"
  chown -R postgres "$RUNDIR"
  cp "$ROOT/supabase/setup.sql" "$RUNDIR/setup.sql"
  cp "$ROOT/supabase/tests/schema_test.sql" "$RUNDIR/schema_test.sql"
  cp "$ROOT/supabase/tests/stub_auth.sql" "$RUNDIR/stub_auth.sql"
  chown postgres "$RUNDIR"/*.sql
  exec su postgres -c "RUNDIR='$RUNDIR' PGPORT='$PORT' PGBIN='$PGBIN' STANDALONE=1 bash '$ROOT/supabase/tests/run.sh'"
fi

if [ "${STANDALONE:-0}" != "1" ]; then
  mkdir -p "$RUNDIR"
  cp "$ROOT/supabase/setup.sql" "$RUNDIR/setup.sql"
  cp "$ROOT/supabase/tests/schema_test.sql" "$RUNDIR/schema_test.sql"
  cp "$ROOT/supabase/tests/stub_auth.sql" "$RUNDIR/stub_auth.sql"
fi

if [ ! -s "$RUNDIR/data/PG_VERSION" ]; then
  initdb -D "$RUNDIR/data" -U postgres --auth=trust >/dev/null
fi

if ! pg_isready -h "$RUNDIR" -p "$PORT" >/dev/null 2>&1; then
  pg_ctl -D "$RUNDIR/data" -l "$RUNDIR/pg.log" \
    -o "-k $RUNDIR -h '' -p $PORT" -w start >/dev/null
fi

psql -h "$RUNDIR" -p "$PORT" -U postgres -d postgres -qc "drop database if exists $DB"
psql -h "$RUNDIR" -p "$PORT" -U postgres -d postgres -qc "create database $DB"

run() { psql -h "$RUNDIR" -p "$PORT" -U postgres -d "$DB" -v ON_ERROR_STOP=1 -q "$@"; }

run -f "$RUNDIR/stub_auth.sql"
echo "── setup.sql 적용 ───────────────────────────────────────────────"
run -f "$RUNDIR/setup.sql" >/dev/null
echo "── setup.sql 재적용 (다시 붙여넣어도 안전한가) ──────────────────"
run -f "$RUNDIR/setup.sql" >/dev/null
echo "OK"
run -f "$RUNDIR/schema_test.sql"
