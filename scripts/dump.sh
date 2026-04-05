#!/usr/bin/env bash
set -euo pipefail

# Load .env if present so MYSQL_* vars are available
if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

DB_NAME=${MYSQL_DATABASE:-soccer_analytics_db}
DB_HOST=${MYSQL_HOST:-localhost}
DB_PORT=${MYSQL_PORT:-3306}
DB_USER=${MYSQL_USER:-root}
DB_PASSWORD=${MYSQL_PASSWORD:-}
OUT_DIR=${OUT_DIR:-dump}
OUT_FILE=${OUT_FILE:-soccer_analytics_db_dump.sql}

mkdir -p "$OUT_DIR"

PASSWORD_OPT=""
if [ -n "$DB_PASSWORD" ]; then
  PASSWORD_OPT="--password=$DB_PASSWORD"
fi

mysqldump \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --user="$DB_USER" \
  $PASSWORD_OPT \
  --databases "$DB_NAME" \
  --routines --triggers --events \
  --add-drop-database --add-drop-table \
  > "$OUT_DIR/$OUT_FILE"

echo "Dump created at $OUT_DIR/$OUT_FILE"
