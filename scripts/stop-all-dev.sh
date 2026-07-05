#!/usr/bin/env bash
# Stop processes started by start-all-dev.sh and docker compose stack.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [ -f /tmp/banking-forms-backend.pid ]; then
  kill "$(cat /tmp/banking-forms-backend.pid)" 2>/dev/null || true
  rm -f /tmp/banking-forms-backend.pid
fi
lsof -ti tcp:8080 | xargs kill -9 2>/dev/null || true
pkill -f "bootRun" 2>/dev/null || true

lsof -ti tcp:5173 | xargs kill -9 2>/dev/null || true
lsof -ti tcp:5174 | xargs kill -9 2>/dev/null || true

docker compose --profile mcp --profile observability down 2>/dev/null || docker compose down 2>/dev/null || true

echo "Stopped backend, frontends, and docker compose services."
