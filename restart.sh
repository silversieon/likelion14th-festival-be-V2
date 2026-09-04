#!/bin/bash
# ============================================================
# Canary + Rolling Replacement
#
# Usage:
#   ./restart.sh 01
#   ./restart.sh 02
#   ./restart.sh 03
# ============================================================

echo "Building..."
./gradlew build -x test

cd docker/local
set -e

TARGET=$1

if [ -z "$TARGET" ]; then
  echo ""
  echo "Usage: restart.sh [01|02|03]"
  echo ""
  exit 1
fi

if [ "$TARGET" != "01" ] && [ "$TARGET" != "02" ] && [ "$TARGET" != "03" ]; then
  echo ""
  echo "Invalid target: $TARGET"
  echo "Usage: restart.sh [01|02|03]"
  echo ""
  exit 1
fi

APP="festival-app-${TARGET}"

echo ""
echo "============================================================"
echo "Canary Deployment"
echo "Target: $APP"
echo "============================================================"
echo ""

# ============================================================
# 1. Remove target instance from Nginx traffic
# ============================================================

echo "[1/6] Removing $APP from traffic..."

{
  echo "upstream festival-app {"
  for n in 01 02 03; do
    if [ "$n" == "$TARGET" ]; then
      echo "    server festival-app-${n}:8080 down;"
    else
      echo "    server festival-app-${n}:8080;"
    fi
  done
  echo "}"
} > nginx/conf.d/upstream-app.conf

if ! docker exec nginx nginx -t; then
  echo ""
  echo "Nginx configuration test failed."
  exit 1
fi

if ! docker exec nginx nginx -s reload; then
  echo ""
  echo "Nginx reload failed."
  exit 1
fi

echo "Target instance removed from traffic."

# ============================================================
# 2. Rebuild and restart only target instance
# ============================================================

echo ""
echo "[2/6] Rebuilding $APP..."

if ! docker compose \
  --compatibility \
  -f docker-compose-local.yml \
  -f docker-compose-app-01.yml \
  -f docker-compose-app-02.yml \
  -f docker-compose-app-03.yml \
  up -d --build --no-deps "$APP"; then
  echo ""
  echo "Failed to rebuild $APP."
  exit 1
fi

echo "$APP rebuilt successfully."

# ============================================================
# 3. Health Check
# ============================================================

echo ""
echo "[3/6] Health checking $APP..."

case "$TARGET" in
  01) PORT=8080 ;;
  02) PORT=8081 ;;
  03) PORT=8082 ;;
esac

HEALTH_OK=0

for i in $(seq 1 60); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/actuator/health" || true)

  if [ "$STATUS" == "200" ]; then
    HEALTH_OK=1
    echo "Health check passed. [$i/60]"
    break
  fi

  echo "Waiting for $APP... [$i/60] status=$STATUS"
  sleep 5
done

if [ "$HEALTH_OK" == "0" ]; then
  echo ""
  echo "========================================================"
  echo "Health check FAILED."
  echo "$APP will remain out of traffic."
  echo "========================================================"
  exit 1
fi

# ============================================================
# 4. Canary traffic
#
# Target: weight 2
# Others: weight 9
#
# 2 / (2 + 9 + 9) = 10%
# ============================================================

echo ""
echo "[4/6] Starting Canary traffic..."

{
  echo "upstream festival-app {"
  for n in 01 02 03; do
    if [ "$n" == "$TARGET" ]; then
      echo "    server festival-app-${n}:8080 weight=2;"
    else
      echo "    server festival-app-${n}:8080 weight=9;"
    fi
  done
  echo "}"
} > nginx/conf.d/upstream-app.conf

if ! docker exec nginx nginx -t; then
  echo ""
  echo "Nginx configuration test failed."
  exit 1
fi

if ! docker exec nginx nginx -s reload; then
  echo ""
  echo "Nginx reload failed."
  exit 1
fi

echo ""
echo "============================================================"
echo "Canary traffic started."
echo "Target: $APP"
echo "Approximate traffic: 10%"
echo "============================================================"
echo ""

# ============================================================
# 5. Observation
# ============================================================

echo "[5/6] Canary observation"
echo ""
echo "Check:"
echo "  - Application logs"
echo "  - HTTP 5xx"
echo "  - Response latency"
echo "  - Prometheus"
echo "  - Grafana"
echo ""
read -n 1 -s -r -p "Press any key after verification..."
echo ""

# ============================================================
# 6. Restore equal traffic
# ============================================================

echo ""
echo "[6/6] Restoring normal traffic..."

{
  echo "upstream festival-app {"
  echo "    server festival-app-01:8080;"
  echo "    server festival-app-02:8080;"
  echo "    server festival-app-03:8080;"
  echo "}"
} > nginx/conf.d/upstream-app.conf

if ! docker exec nginx nginx -t; then
  echo ""
  echo "Nginx configuration test failed."
  exit 1
fi

if ! docker exec nginx nginx -s reload; then
  echo ""
  echo "Nginx reload failed."
  exit 1
fi

echo ""
echo "============================================================"
echo "Canary deployment completed successfully."
echo "Target: $APP"
echo "Traffic: 33% / 33% / 33%"
echo "============================================================"
echo ""