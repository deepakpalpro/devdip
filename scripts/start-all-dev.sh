#!/usr/bin/env bash
# Start the full local dev stack for banking-forms-platform.
#
# Usage:
#   ./scripts/start-all-dev.sh              # backend + frontends + docker integrations
#   ./scripts/start-all-dev.sh --obs        # + Prometheus/Grafana
#   ./scripts/start-all-dev.sh --mcp        # + MCP HTTP server (Docker :3100)
#   ./scripts/start-all-dev.sh --obs --mcp  # everything
#
# Stop: ./scripts/stop-all-dev.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

OBS=false
MCP=false
for arg in "$@"; do
  case "$arg" in
    --obs|--observability) OBS=true ;;
    --mcp) MCP=true ;;
  esac
done

echo "=== 1/4 Docker integrations ==="
DOCKER_ARGS=()
$OBS && DOCKER_ARGS+=(--obs)
$MCP && DOCKER_ARGS+=(--mcp)
./scripts/docker-up.sh "${DOCKER_ARGS[@]}"

echo ""
echo "=== 2/4 Backend (Spring Boot :8080) ==="
if lsof -ti tcp:8080 >/dev/null 2>&1; then
  echo "Port 8080 already in use — skipping bootRun"
else
  ./gradlew bootRun > /tmp/banking-forms-backend.log 2>&1 &
  echo $! > /tmp/banking-forms-backend.pid
  echo "Started backend pid $(cat /tmp/banking-forms-backend.pid) → /tmp/banking-forms-backend.log"
  for i in $(seq 1 30); do
    if curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; then
      echo "Backend UP"
      if $OBS; then
        echo "Configuring downstream providers (rest-webhook + kafka-stream)..."
        DOWNSTREAM_WEBHOOK_TOKEN="${DOWNSTREAM_WEBHOOK_TOKEN:-dev-webhook-key}" ./scripts/configure-dev-downstream.sh >/dev/null || true
      fi
      break
    fi
    sleep 2
  done
fi

echo ""
echo "=== 3/4 Frontends (:5173 consumer, :5174 admin) ==="
if [ ! -d frontend/node_modules ]; then
  (cd frontend && npm install)
fi
if ! lsof -ti tcp:5173 >/dev/null 2>&1; then
  (cd frontend && npm run dev:consumer > /tmp/banking-forms-consumer.log 2>&1 &)
  echo "Consumer portal → /tmp/banking-forms-consumer.log"
fi
if ! lsof -ti tcp:5174 >/dev/null 2>&1; then
  (cd frontend && npm run dev:admin > /tmp/banking-forms-admin.log 2>&1 &)
  echo "Admin portal → /tmp/banking-forms-admin.log"
fi

echo ""
echo "=== 4/4 MCP server ==="
if $MCP; then
  echo "MCP HTTP running via Docker profile on :3100 (see docker compose ps)"
else
  echo "Building MCP server for Cursor stdio use..."
  (cd mcp-server && npm install --silent && npm run build --silent)
  echo "Configure Cursor with mcp-server/cursor-mcp.example.json (see docs/MCP_TECHNICAL_GUIDE.md)"
fi

echo ""
echo "============================================"
echo "  Banking Forms Platform — dev stack"
echo "============================================"
echo "  Backend     http://localhost:8080"
echo "  Consumer    http://localhost:5173"
echo "  Admin       http://localhost:5174"
echo "  Swagger     http://localhost:8080/swagger-ui.html"
echo "  Ollama      http://localhost:11434"
echo "  Kafka       localhost:9092"
echo "  LocalStack  http://localhost:4566"
$MCP && echo "  MCP HTTP    http://localhost:3100/mcp  (Bearer dev-mcp-key)"
$OBS && echo "  Prometheus  http://localhost:9090"
$OBS && echo "  Grafana     http://localhost:3000  (admin/admin)"
echo ""
echo "  MCP UAT guide: docs/MCP_TECHNICAL_GUIDE.md"
echo "  Stop all:      ./scripts/stop-all-dev.sh"
echo "============================================"
