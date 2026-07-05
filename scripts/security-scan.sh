#!/usr/bin/env bash
# OWASP dependency vulnerability scan (US-9.3).
set -euo pipefail
cd "$(dirname "$0")/.."
echo "Running OWASP dependency-check (may take several minutes on first run)..."
./gradlew dependencyCheckAnalyze --no-daemon
echo "Report: build/reports/dependency-check-report.html"
