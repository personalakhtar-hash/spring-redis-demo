#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
for port in 8081 8082; do
  file="$ROOT/.runtime/app-$port.pid"
  if [[ -f "$file" ]]; then
    pid="$(cat "$file")"
    if ps -p "$pid" -o command= | grep -Fq 'spring-redis-demo-0.0.1-SNAPSHOT.jar'; then kill "$pid"; fi
    rm -f "$file"
  fi
done
if [[ -f "$ROOT/.runtime/redis.pid" ]]; then
  pid="$(cat "$ROOT/.runtime/redis.pid")"
  if ps -p "$pid" -o command= | grep -Fq 'redis-server'; then kill "$pid"; fi
fi
if pg_ctl -D "$ROOT/.runtime/postgres" status >/dev/null 2>&1; then
  pg_ctl -D "$ROOT/.runtime/postgres" -m fast -w stop
fi
