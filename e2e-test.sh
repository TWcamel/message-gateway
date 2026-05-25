#!/usr/bin/env bash
# =============================================================================
# End-to-end test for: curl → gcp-mnp → Pub/Sub → nats-adapter → NATS → nats-consumer (log)
#
# Usage:
#   ./e2e-test.sh           # clean run, removes containers on exit
#   ./e2e-test.sh --keep    # keep containers after run (for debugging)
#
# Pre-requisites:
#   - Docker + docker compose available
#   - mnp-commons already installed to local Maven repo
#     (run: cd commons && mvn -B clean install -DskipTests)
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

KEEP_CONTAINERS=false
if [[ "${1:-}" == "--keep" ]]; then
  KEEP_CONTAINERS=true
fi

cleanup() {
  if [[ "$KEEP_CONTAINERS" == "true" ]]; then
    echo "[e2e] --keep flag set; containers left running. Run 'docker compose down' manually."
    return
  fi
  echo "[e2e] Tearing down docker compose stack..."
  docker compose down -v --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

# ──────────────────────────────────────────────────────────────
# 1. Ensure commons is installed
# ──────────────────────────────────────────────────────────────
if [[ ! -f "$HOME/.m2/repository/com/mnp/mnp-commons/0.0.1-SNAPSHOT/mnp-commons-0.0.1-SNAPSHOT.jar" ]]; then
  echo "[e2e] mnp-commons not installed. Building..."
  ( cd commons && mvn -B -q install -DskipTests )
fi

# ──────────────────────────────────────────────────────────────
# 2. Copy local Maven repo into docker build context (each service dir)
#    so Dockerfiles can resolve mnp-commons without network access.
# ──────────────────────────────────────────────────────────────
M2_REPO="$HOME/.m2/repository"
echo "[e2e] Staging local Maven repo for Docker builds..."
for svc in gcp-mnp nats-adapter nats-consumer; do
  rm -rf "$SCRIPT_DIR/$svc/.m2"
  # Sparse copy: only copy mnp-commons artifact (saves space / time)
  mkdir -p "$SCRIPT_DIR/$svc/.m2/com/mnp"
  cp -R "$M2_REPO/com/mnp/mnp-commons" "$SCRIPT_DIR/$svc/.m2/com/mnp/"
done

# ──────────────────────────────────────────────────────────────
# 3. Start stack
# ──────────────────────────────────────────────────────────────
echo "[e2e] docker compose build..."
docker compose build --quiet
echo "[e2e] docker compose up -d..."
docker compose up -d

# ──────────────────────────────────────────────────────────────
# 4. Wait for services to become healthy
# ──────────────────────────────────────────────────────────────
wait_healthy() {
  local svc="$1"
  local max_wait="${2:-180}"
  local waited=0
  echo "[e2e] Waiting for $svc to be healthy (max ${max_wait}s)..."
  while (( waited < max_wait )); do
    status=$(docker compose ps --format json "$svc" 2>/dev/null \
             | python3 -c "import sys,json; print(json.loads(sys.stdin.read()).get('Health',''))" 2>/dev/null || echo "")
    if [[ "$status" == "healthy" ]]; then
      echo "[e2e] $svc is healthy after ${waited}s"
      return 0
    fi
    sleep 3
    waited=$((waited + 3))
  done
  echo "[e2e] TIMEOUT waiting for $svc (last status=$status)" >&2
  return 1
}

wait_healthy gcp-mnp     240 || { docker compose logs gcp-mnp;     exit 1; }
wait_healthy nats-adapter 240 || { docker compose logs nats-adapter; exit 1; }
wait_healthy nats-consumer 240 || { docker compose logs nats-consumer; exit 1; }

# Extra settle time for NATS subscriptions / Pub/Sub subscribers to wire up
sleep 5

# ──────────────────────────────────────────────────────────────
# 5. Publish a message
# ──────────────────────────────────────────────────────────────
echo "[e2e] Publishing message via gcp-mnp..."
RESPONSE=$(curl -sS -X POST http://localhost:8080/api/v1/publish)
echo "[e2e] Response: $RESPONSE"

# Extract messageId
MESSAGE_ID=$(echo "$RESPONSE" | python3 -c "import sys,json; r=json.loads(sys.stdin.read()); print(r.get('data',{}).get('messageId',''))")
if [[ -z "$MESSAGE_ID" ]]; then
  echo "[e2e] ERROR: Could not extract messageId from response" >&2
  docker compose logs gcp-mnp
  exit 1
fi
echo "[e2e] messageId = $MESSAGE_ID"

# ──────────────────────────────────────────────────────────────
# 6. Poll nats-consumer logs until messageId appears
# ──────────────────────────────────────────────────────────────
MAX_POLL=60
WAITED=0
echo "[e2e] Polling nats-consumer logs for messageId (max ${MAX_POLL}s)..."
while (( WAITED < MAX_POLL )); do
  if docker compose logs --since=60s nats-consumer 2>/dev/null | grep -qF "$MESSAGE_ID"; then
    echo "[e2e] Found messageId in nats-consumer logs ✓"
    docker compose logs --since=60s nats-consumer | grep -F "$MESSAGE_ID" | head -3
    echo "[e2e] PASSED"
    exit 0
  fi
  sleep 2
  WAITED=$((WAITED + 2))
done

echo "[e2e] FAILED: messageId not observed in nats-consumer within ${MAX_POLL}s" >&2
echo "[e2e] --- nats-consumer logs ---"
docker compose logs --tail=50 nats-consumer
echo "[e2e] --- nats-adapter logs ---"
docker compose logs --tail=50 nats-adapter
exit 1
