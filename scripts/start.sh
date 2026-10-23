#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
cd "$ROOT"
if [[ ! -f .runtime/postgres/PG_VERSION ]]; then
  initdb -D .runtime/postgres -U cache_demo --auth-local=trust --auth-host=scram-sha-256 --pwfile=<(printf '%s\n' "$PGPASSWORD") > .runtime/initdb.log
fi
if ! pg_ctl -D .runtime/postgres status >/dev/null 2>&1; then
  pg_ctl -D .runtime/postgres -l "$ROOT/.runtime/postgres.log" -o "-p 5433 -h 127.0.0.1 -k '$ROOT/.runtime'" -w start
fi
if ! psql -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='cache_demo'" | tr -d ' ' | grep -q '^1$'; then
  createdb cache_demo
fi
if [[ ! -f .runtime/redis.pid ]] || ! kill -0 "$(cat .runtime/redis.pid)" 2>/dev/null; then
  redis-server --bind 127.0.0.1 --port 6380 --daemonize yes --pidfile "$ROOT/.runtime/redis.pid" --dir "$ROOT/.runtime" --appendonly yes --logfile "$ROOT/.runtime/redis.log"
fi
mvn -B package > .runtime/build.log 2>&1 || { tail -60 .runtime/build.log; exit 1; }
for port in 8081 8082; do
  if [[ -f ".runtime/app-$port.pid" ]] && kill -0 "$(cat ".runtime/app-$port.pid")" 2>/dev/null; then
    echo "Instance $port already running"
    continue
  fi
  nohup "$JAVA_HOME/bin/java" -jar target/spring-redis-demo-0.0.1-SNAPSHOT.jar --server.port="$port" > ".runtime/app-$port.log" 2>&1 < /dev/null &
  echo $! > ".runtime/app-$port.pid"
  ready=false
  for attempt in {1..90}; do
    if curl -fsS "http://localhost:$port/actuator/health" > /dev/null 2>&1; then ready=true; break; fi
    if ! kill -0 "$(cat ".runtime/app-$port.pid")" 2>/dev/null; then break; fi
    sleep 1
  done
  if [[ "$ready" != true ]]; then tail -60 ".runtime/app-$port.log"; exit 1; fi
  echo "Ready: http://localhost:$port"
done
