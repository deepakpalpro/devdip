#!/usr/bin/env bash
# Enable local downstream connectors (rest-webhook + kafka-stream) for dev/UAT.
#
# Prerequisites:
#   - Backend running on :8080
#   - docker compose up (kafka :9092, webhook-sink :8099)
#   - Optional: export DOWNSTREAM_WEBHOOK_TOKEN=dev-webhook-key before starting backend
#
# Usage: ./scripts/configure-dev-downstream.sh
set -euo pipefail

API="${BANKING_API_URL:-http://localhost:8080}"
TENANT="${BANKING_TENANT_ID:-11111111-1111-1111-1111-111111111111}"
WEBHOOK_ENDPOINT="${DOWNSTREAM_WEBHOOK_ENDPOINT:-http://localhost:8099/post}"
KAFKA_BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}"
KAFKA_TOPIC="${KAFKA_TOPIC_SUBMISSIONS:-submissions.processed}"

put() {
  local code="$1"
  local body="$2"
  curl -sf -X PUT "$API/api/admin/v1/downstream-providers/$code" \
    -H "X-Tenant-Id: $TENANT" \
    -H "Content-Type: application/json" \
    -d "$body" | python3 -m json.tool
}

echo "=== Enabling rest-webhook → $WEBHOOK_ENDPOINT ==="
put rest-webhook "$(cat <<EOF
{
  "enabled": true,
  "priority": 20,
  "config": {
    "endpoint": "$WEBHOOK_ENDPOINT",
    "method": "POST",
    "secretRef": "DOWNSTREAM_WEBHOOK_TOKEN"
  }
}
EOF
)"

echo ""
echo "=== Enabling kafka-stream → $KAFKA_BOOTSTRAP / $KAFKA_TOPIC ==="
put kafka-stream "$(cat <<EOF
{
  "enabled": true,
  "priority": 30,
  "config": {
    "bootstrapServers": "$KAFKA_BOOTSTRAP",
    "topic": "$KAFKA_TOPIC"
  }
}
EOF
)"

echo ""
echo "=== Current downstream providers ==="
curl -sf "$API/api/admin/v1/downstream-providers" -H "X-Tenant-Id: $TENANT" | python3 -m json.tool

echo ""
echo "Done. Submit a new form to fan out to log-sink + rest-webhook + kafka-stream."
echo "  Webhook logs: docker compose logs -f webhook-sink"
echo "  Kafka topic:  docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic $KAFKA_TOPIC --from-beginning"
