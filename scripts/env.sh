#!/usr/bin/env bash
if [[ -n "${BASH_VERSION:-}" ]]; then
  demo_env_file="${BASH_SOURCE[0]}"
else
  demo_env_file="${(%):-%x}"
fi
ROOT="$(cd "$(dirname "$demo_env_file")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
export PATH="$JAVA_HOME/bin:/opt/homebrew/opt/postgresql@17/bin:/opt/homebrew/bin:$PATH"
export PGHOST=127.0.0.1 PGPORT=5433 PGUSER=cache_demo PGPASSWORD=local_demo_password PGDATABASE=cache_demo
mkdir -p "$ROOT/.runtime"
