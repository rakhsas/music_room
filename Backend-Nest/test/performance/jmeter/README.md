# V.7 ramp-up load test (JMeter)

## Setup (once per DB seed)

1. Start the server (`npm run start:dev` in `Backend-Nest`) and Postgres (`docker compose up db`).
2. From this folder: `./setup-fixtures.sh` — creates 5 throwaway accounts, logs them in, and writes
   `tokens.csv` (gitignored — real JWTs, never commit it). Also creates the fixtures the test plan
   targets: event id 4 (with attendees + a track to vote on), device `loadtest-device-1` (delegated
   to all 5 users), playlist id 4 (all 5 users have edit permission).
3. If your DB isn't fresh, the created ids might not be 4/`loadtest-device-1`/4 — either edit the
   `HTTPSampler.path` values in `musicroom-rampup.jmx` (see below) to match, or reset your DB first.

## Running it

```
jmeter -n -t musicroom-rampup.jmx -Jthreads=10 -Jrampup=2 -Jloops=15 -l results/level-10.jtl
```

- `-n` = non-GUI (headless) mode. `-t` = the test plan file.
- `-Jthreads=N` = concurrent virtual users. `-Jrampup=S` = seconds to spin all of them up over
  (avoid an instant burst). `-Jloops=L` = how many times each thread repeats its requests.
- `-l results/level-10.jtl` = write raw per-request results (CSV) to this file. `results/` is
  gitignored.

Re-run with increasing `-Jthreads` (e.g. 10, 25, 50, 100, 200) to build a ramp — that's what "V.7
ramp-up" means: find the concurrency level where latency/errors start to break down, not just run
it once.

To open the plan in the GUI instead (easier to poke around in before your first headless run):
`jmeter -t musicroom-rampup.jmx` (no `-n`).

## Reading the results

Each `.jtl` is a CSV: one row per HTTP request, columns include `timeStamp`, `elapsed` (ms),
`label` (which sampler), `responseCode`, `success`. Open it in anything that reads CSV, or use
JMeter's own report generator for a full HTML dashboard:

```
jmeter -g results/level-10.jtl -o results/level-10-report
```

Then open `results/level-10-report/index.html`.

## How the test plan (`musicroom-rampup.jmx`) is put together

It's XML, but the structure maps directly onto the JMeter GUI's tree (open it with `jmeter -t
musicroom-rampup.jmx` to see this visually — much easier to follow than the raw XML):

```
Test Plan
├─ HTTP Request Defaults        <- host/port (localhost:8000), so samplers below only need a path
├─ CSV Data Set Config          <- reads tokens.csv, exposes ${token} and ${userId} per thread
├─ Auth Headers                 <- Authorization: Bearer ${token} on every request
├─ Thread Group "1 - Music Track Vote"
│   ├─ POST vote     -> /api/events/4/tracks/jamendo-track-1/vote
│   └─ DELETE unvote -> /api/events/4/tracks/jamendo-track-1/unvote
├─ Thread Group "2 - Device Control Delegation"
│   ├─ POST control/play  -> /api/devices/loadtest-device-1/control/play
│   └─ POST control/pause -> /api/devices/loadtest-device-1/control/pause
└─ Thread Group "3 - Playlist Editor"
    ├─ Counter Config "trackNum" <- increments once per loop iteration, per thread
    ├─ POST tracks/add    -> /api/playlists/4/tracks/load-${__threadNum}-${trackNum}/add
    └─ DELETE tracks/remove -> /api/playlists/4/tracks/load-${__threadNum}-${trackNum}/remove
```

**Thread Group** = one group of virtual users hitting a sequence of requests in a loop. Each of the
3 services gets its own Thread Group, all 3 run concurrently in the same test.

**Why the Counter Config in Thread Group 3 only**: services 1 and 2 always hit the exact same URL
(vote the same track, control the same device) on purpose — that's what stresses the
read-modify-write race described in `PROJECT_STATUS.md` V.2. Service 3 instead needs a *fresh*
track id on every add, otherwise the second+ loop iteration just no-ops ("track already in
playlist") instead of doing real work. `${__threadNum}` (built-in JMeter function, the thread's own
number) plus `${trackNum}` (the Counter Config's value, incremented once per loop) together produce
a unique id per thread per iteration — and because the counter only increments once per loop, both
the `add` and the paired `remove` in that same iteration read the same value, so they target the
same track. (An earlier version of this plan tried to do this with two separate
`${__counter(...)}` function calls instead of a Counter Config element — don't do that, each call
to `__counter()` increments independently, so `add` and `remove` end up with *different* ids and
`remove` becomes a permanent no-op.)

**Known finding**: running Thread Group 3 leaves stray tracks in playlist id 4 even when every
request succeeds (200/201) — this is a real lost-update bug in `playlists.service.ts`
(`addTrack`/`removeTrack` do `load → mutate → save` with no transaction/lock), not a bug in this
test plan. See `PROJECT_STATUS.md` V.2/V.7. Reset the fixture before a clean rerun:

```
docker exec postgres_db psql -U postgres -d musicroom -c "UPDATE playlists SET tracks='[\"jamendo-track-1\"]' WHERE id=4;"
```

## Gotchas that bit us during setup

- **Login is now rate-limited to 5/min/IP** (the V.6 brute-force fix). `setup-fixtures.sh` logs in
  exactly 5 times in one batch — if you've been poking at `/users/login` manually right before
  running it, you'll get `429`s and empty/broken tokens. Wait a minute and re-run.
- **JWTs expire in 1 hour.** If `musicroom-rampup.jmx` suddenly starts getting 401s on every
  request, `tokens.csv` is stale — re-run `setup-fixtures.sh`.
- **`enabled="..."` on an XML element is evaluated when JMeter loads the file, not at run time** —
  you can't use a JMeter property function (`${__P(...)}`) there to toggle a Thread Group on/off.
  It has to be a literal `true`/`false`. (All 3 Thread Groups here are hardcoded `enabled="true"`.)
