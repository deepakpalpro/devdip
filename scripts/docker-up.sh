#!/usr/bin/env bash
# Start local integration stack (Ollama, Kafka, LocalStack).
# Usage:
#   ./scripts/docker-up.sh              # integrations only
#   ./scripts/docker-up.sh --obs        # + Prometheus/Grafana
#   ./scripts/docker-up.sh --pull       # force Ollama model re-pull
set -euo pipefail
cd "$(dirname "$0")/.."

PROFILES=()
EXTRA=()

for arg in "$@"; do
  case "$arg" in
    --obs|--observability)
      PROFILES+=(--profile observability)
      ;;
    --pull)
      export OLLAMA_MODELS="${OLLAMA_MODELS:-llava llama3.2}"
      docker compose rm -sf ollama-init 2>/dev/null || true
      ;;
    *)
      EXTRA+=("$arg")
      ;;
  esac
done

echo "Starting docker compose (integrations: ollama :11434, kafka :9092, localstack :4566)..."
docker compose up -d "${PROFILES[@]}" "${EXTRA[@]}"

echo ""
echo "Waiting for Ollama model init (may take several minutes on first run)..."
docker compose logs -f ollama-init &
LOG_PID=$!
if docker compose wait ollama-init 2>/dev/null; then
  kill "$LOG_PID" 2>/dev/null || true
else
  wait "$LOG_PID" 2>/dev/null || true
fi

echo ""
echo "Stack status:"
docker compose ps
echo ""
echo "Endpoints:"
echo "  Ollama      http://localhost:11434   (models: llava, llama3.2)"
echo "  Kafka       localhost:9092            (topic: submissions.processed)"
echo "  LocalStack  http://localhost:4566     (S3 bucket: banking-forms-submissions)"
if [[ " ${PROFILES[*]} " == *"observability"* ]]; then
  echo "  Prometheus  http://localhost:9090"
  echo "  Grafana     http://localhost:3000    (admin/admin)"
fi
