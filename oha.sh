#!/usr/bin/env bash
# usage: ./oha.sh <host> <output_prefix>

set -e

HOST="$1"
OUT_PREFIX="$2"

if [ -z "$HOST" ] || [ -z "$OUT_PREFIX" ]; then
  echo "usage: $0 <host> <output_prefix>"
  exit 1
fi

COUNTS=(1 64 256 1024 2048)

for count in "${COUNTS[@]}"; do
  oha \
    --http-version 1.1 \
    --no-tui \
    -c 100 \
    -z 15s \
    "http://${HOST}/${count}" \
    -o "benchmark/${OUT_PREFIX}_${count}_64k_result.txt"
done