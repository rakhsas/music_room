#!/usr/bin/env bash
# Seeds 5 throwaway accounts + one event/device/playlist fixture for the
# ramp-up load tests (ramp-up.js). Idempotent-ish: safe to re-run, registration/
# grant calls that already happened just 400/no-op.
#
# Requires: the Nest server running on $BASE_URL, and the "postgres_db"
# container from docker-compose.yml running (used to flip isActive/isVerified
# directly - these test accounts never receive a real verification email).
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8000/api}"
DB_CONTAINER="${DB_CONTAINER:-postgres_db}"
# cd into this script's own dir and use bare relative filenames from here on -
# node.exe (a native Windows binary) can't resolve the /c/... style absolute
# paths Git Bash hands out, so mixing them into `node -e` strings silently
# breaks (bit us twice already).
cd "$(dirname "${BASH_SOURCE[0]}")"

echo "Registering 5 load-test accounts..."
for i in 1 2 3 4 5; do
  curl -s -o /dev/null -X POST "$BASE_URL/users/create" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"loadtest${i}@example.test\",\"password\":\"LoadTest123!\",\"full_name\":\"Load Test ${i}\",\"username\":\"loadtest${i}\"}"
done

echo "Activating them (bypasses email verification for test-only accounts)..."
docker exec "$DB_CONTAINER" psql -U postgres -d musicroom -c \
  "UPDATE users SET \"isActive\"=true, \"isVerified\"=true WHERE email LIKE 'loadtest%@example.test';"

echo "Logging in and writing tokens.csv (gitignored - real JWTs, never commit)..."
rm -rf .tmp && mkdir .tmp
for i in 1 2 3 4 5; do
  curl -s -X POST "$BASE_URL/users/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"loadtest${i}@example.test\",\"password\":\"LoadTest123!\"}" \
    -o ".tmp/${i}.json"
done
node -e '
  const fs = require("fs");
  const rows = ["token,userId"];
  for (let i = 1; i <= 5; i++) {
    const r = JSON.parse(fs.readFileSync(".tmp/" + i + ".json", "utf8"));
    if (!r.tokens) throw new Error("login " + i + " failed: " + JSON.stringify(r));
    rows.push(r.tokens.access + "," + r.user.id);
  }
  fs.writeFileSync("tokens.csv", rows.join("\n") + "\n");
'
rm -rf .tmp

TOKEN1=$(node -e "console.log(require('fs').readFileSync('tokens.csv','utf8').split('\n')[1].split(',')[0])")
USER_IDS=$(tail -n +2 "tokens.csv" | cut -d, -f2)
USER2_ID=$(echo "$USER_IDS" | sed -n '2p')
USER3_ID=$(echo "$USER_IDS" | sed -n '3p')
USER4_ID=$(echo "$USER_IDS" | sed -n '4p')
USER5_ID=$(echo "$USER_IDS" | sed -n '5p')

echo "Creating fixture event (Music Track Vote target)..."
curl -s -X POST "$BASE_URL/events/create" -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN1" \
  -d '{"title":"Load Test Event","location":"E1","eventStartTime":"2026-09-21T20:00:00Z","isPublic":true}'
echo

echo "Joining users 2-5 to the event..."
for i in 2 3 4 5; do
  TOK=$(sed -n "$((i+1))p" "tokens.csv" | cut -d, -f1)
  curl -s -o /dev/null -X POST "$BASE_URL/events/4/join" -H "Authorization: Bearer $TOK"
done

echo "Adding a track to vote on..."
curl -s -X POST "$BASE_URL/events/4/tracks/jamendo-track-1/add" -H "Authorization: Bearer $TOKEN1"
echo

echo "Registering fixture device + delegating to all 5 users (Device Control Delegation target)..."
curl -s -X POST "$BASE_URL/devices/register" -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN1" \
  -d '{"deviceId":"loadtest-device-1","name":"LoadTest Speaker","platform":"Android","appVersion":"1.0.0"}'
echo
for i in 2 3 4 5; do
  curl -s -o /dev/null -X POST "$BASE_URL/devices/loadtest-device-1/delegate" -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN1" \
    -d "{\"email\":\"loadtest${i}@example.test\"}"
done

echo "Creating fixture playlist + granting edit to all 5 users (Playlist Editor target)..."
curl -s -X POST "$BASE_URL/playlists/create" -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN1" \
  -d '{"name":"Load Test Playlist","isPublic":true}'
echo
for uid in "$USER2_ID" "$USER3_ID" "$USER4_ID" "$USER5_ID"; do
  curl -s -o /dev/null -X POST "$BASE_URL/playlists/4/permissions/grant" -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN1" \
    -d "{\"userId\":${uid}}"
done
curl -s -o /dev/null -X POST "$BASE_URL/playlists/4/tracks/jamendo-track-1/add" -H "Authorization: Bearer $TOKEN1"

echo "Done. Fixtures: event id 4, device 'loadtest-device-1', playlist id 4."
echo "If these ids differ on your DB (not a fresh seed), edit ramp-up.js's request paths to match."
