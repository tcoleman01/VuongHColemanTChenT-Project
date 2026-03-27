#!/usr/bin/env bash
set -euo pipefail

DB_NAME=${DB_NAME:-cs5200_player_db}
OUT_DIR=${OUT_DIR:-dump}

mkdir -p "$OUT_DIR"
mongodump --db "$DB_NAME" --out "$OUT_DIR"

echo "Dump created in $OUT_DIR"
