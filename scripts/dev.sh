#!/usr/bin/env bash
# Dev mode, like `npm run dev`:
#   - starts the server (profile dev, port 8080)
#   - watches src/main for changes, recompiles, and Spring DevTools restarts the app in ~1-2 s
#   - Ctrl+C stops everything
#
# Usage: ./scripts/dev.sh
set -uo pipefail
cd "$(dirname "$0")/.."

STAMP="target/.dev-watch-stamp"

if lsof -ti tcp:"${PORT:-8080}" >/dev/null 2>&1; then
  echo "✗ Port ${PORT:-8080} is already in use. Stop the other server first:"
  echo "    lsof -ti tcp:${PORT:-8080} | xargs kill"
  exit 1
fi

echo "▶ Compiling..."
./mvnw -q -B compile || { echo "✗ Compile failed. Fix the errors above and run again."; exit 1; }
touch "$STAMP"

echo "▶ Starting server (Ctrl+C to stop)..."
./mvnw -q -B spring-boot:run &
APP_PID=$!

cleanup() {
  trap - INT TERM EXIT
  echo; echo "■ Stopping server..."
  pkill -TERM -P "$APP_PID" 2>/dev/null          # the app JVM forked by Maven
  kill "$APP_PID" 2>/dev/null                    # Maven itself
  # fallback: whatever still listens on the port
  lsof -ti tcp:"${PORT:-8080}" 2>/dev/null | xargs kill 2>/dev/null
  wait "$APP_PID" 2>/dev/null
  echo "■ Stopped."
  exit 0
}
trap cleanup INT TERM EXIT

# Watch loop: poll once a second for files newer than the last compile.
while kill -0 "$APP_PID" 2>/dev/null; do
  sleep 1
  if [[ -n "$(find src/main pom.xml -newer "$STAMP" -type f 2>/dev/null | head -1)" ]]; then
    sleep 0.5                       # let the editor finish saving
    touch "$STAMP"
    echo "↻ Change detected, recompiling..."
    if ./mvnw -q -B -o compile; then
      echo "✓ Compiled. Server is restarting..."
    else
      echo "✗ Compile error (server keeps running the last good version). Fix and save again."
    fi
  fi
done
echo "Server process ended."
