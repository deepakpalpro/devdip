#!/usr/bin/env bash
# Basic load baseline for the banking-forms platform (US-9.3).
# Requires: curl, backend running on :8080
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
TENANT="${TENANT_ID:-11111111-1111-1111-1111-111111111111}"
REQUESTS="${REQUESTS:-50}"

echo "Load test: $REQUESTS health checks against $BASE"

pass=0
fail=0
total_ms=0

for i in $(seq 1 "$REQUESTS"); do
  start=$(python3 -c "import time; print(int(time.time()*1000))")
  code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/actuator/health")
  end=$(python3 -c "import time; print(int(time.time()*1000))")
  elapsed=$((end - start))
  total_ms=$((total_ms + elapsed))
  if [ "$code" = "200" ]; then
    pass=$((pass + 1))
  else
    fail=$((fail + 1))
    echo "  request $i failed: HTTP $code"
  fi
done

avg=$((total_ms / REQUESTS))
echo "Results: pass=$pass fail=$fail avg_ms=$avg"
echo "Sample admin list:"
curl -s -o /dev/null -w "  GET /api/admin/v1/forms → %{http_code} (%{time_total}s)\n" \
  -H "X-Tenant-Id: $TENANT" "$BASE/api/admin/v1/forms"

if [ "$fail" -gt 0 ]; then
  exit 1
fi
