# 008 — Isolated local demo database and catalogue JDBC verification

Date: 26 September 2026
Contributor: Johannsen
Branch and starting revision: attendee, 04dfc57, with preserved uncommitted work
Agent/tool: Codex desktop, single agent
Skills: database-migration-and-integrity, security-and-rbac, and installed
supabase-postgres-best-practices (privileges, constraints, short transactions).

## Objective and original prompt

The user supplied successful Gradle output for
`./gradlew test --tests 'seedu.eventmanager.attendee.*' --rerun-tasks`, including
unchecked-operation/deprecation warnings and BUILD SUCCESSFUL, then requested:

> These are the results, also lets create a db for it so that i cna see other details right in club organiser and also venue admin

The preceding pasted terminal output is summarized above rather than reconstructed.

## Decisions and response summary

- Verified the user's XML report: 9 tests, 6 passed and 3 DB tests skipped.
- Existing localhost:5432 connection with no password was refused. Did not read
  credentials, alter its authentication, overwrite data, or change `.env`.
- Used installed Postgres.app 17.4 to create a separate local cluster under
  `.local-demo/postgres`, bound only to 127.0.0.1:55448, with Unix sockets disabled.
- Created `mp2_demo_johannsen` and a separate `mp2_attendee_test_johannsen` database.
- App owner `mp2_demo_app` has no superuser/create-db/create-role privileges.
  Cluster bootstrap account is separate. Loopback trust auth is explicitly a
  synthetic local-only prototype, not a secure multi-user or network deployment.
- Applied existing Flyway V1–V4 plus Organizer V1 unchanged. No product schema or
  teammate business-rule changes. Fixture insertion is one transaction; the
  migration setup precedes it. Refuse reseeding nonempty data.
- Created demo-admin through the existing local-session/password hashing service,
  granted its venue access, and generated a random password into a private
  mode-600 file inside a mode-700 directory. No password/hash/session token was
  printed in tools, logs or chat. Entire local demo directory is Git-excluded.
- Sample data: 3 venues; 6 events (2 future published, 3 drafts, 1 past published);
  2 approved requests/bookings, 2 pending requests (one conflicts), 1 availability
  block. Seeded states explicitly describe synthetic fixtures, not real user
  actions or implemented event publication. No fabricated business audits.
- Launcher overrides configuration only for its child process, preserving `.env`.
  Existing processes need closing/relaunching to use the demo. Data persists.

## Files changed

- Local Git exclusion: `.git/info/exclude`, adding `/.local-demo/`.
- Machine-local ignored files: `.local-demo/SeedDemo.java`, `run.sh`, `test.sh`,
  `stop.sh`, `README.md`, private password file, PostgreSQL data/logs.
- This interaction record. No tracked application code, dependencies, migrations
  or user environment configuration changed in this task.

## Commands and results

Working directory: repository root, unless otherwise specified.

- Read AGENTS, relevant skills and their selected references, schemas, database
  bootstrap, session/password service, Organizer IDs and service, and venue
  workflow/composition sources.
- Parsed `build/test-results/test/TEST-*.xml` using Python ElementTree: initial
  9 tests/3 skipped/0 failures/0 errors.
- `psql -X -w -h localhost -p 5432 -U johannsenlum -d postgres -Atc "SELECT current_user, current_setting('server_version'); SELECT datname FROM pg_database WHERE datname IN ('mp2_demo_johannsen','mp2_attendee_test_johannsen');"`:
  authentication refused because no password supplied. No change on that server.
- `/Applications/Postgres.app/Contents/Versions/17/bin/pg_isready -h 127.0.0.1 -p 55448`:
  no response before setup. `postgres --version`: 17.4.
- `chmod 700 .local-demo` and `mkdir -m 700 .local-demo/socket`:
  local private directories created; socket directory remains unused.
- `/Applications/Postgres.app/Contents/Versions/17/bin/initdb -D .local-demo/postgres -U mp2_bootstrap --auth-local=trust --auth-host=trust --encoding=UTF8 --locale=C`: success.
- `./gradlew installDist`: success, distribution named `event-venue-manager`.
- `/Applications/Postgres.app/Contents/Versions/17/bin/pg_ctl -D .local-demo/postgres -l .local-demo/postgres.log -o "-h 127.0.0.1 -p 55448 -k ''" -w start`: success.
- On that explicit isolated endpoint, psql executed
  `CREATE ROLE mp2_demo_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;`.
- `/Applications/Postgres.app/Contents/Versions/17/bin/createdb -h 127.0.0.1 -p 55448 -U mp2_bootstrap -O mp2_demo_app mp2_demo_johannsen`: success.
- Same createdb command with `mp2_attendee_test_johannsen`: success.
- `java --class-path 'build/install/CS3227-2610-MP2/lib/*' .local-demo/SeedDemo.java`:
  failed compilation because the assumed distribution path was wrong; no seed ran.
- Corrected: `java --class-path 'build/install/event-venue-manager/lib/*' .local-demo/SeedDemo.java`:
  migrations, seed and real service verification succeeded. Credentials not printed.
- `EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_attendee_test_johannsen EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests 'seedu.eventmanager.attendee.*' --rerun-tasks`:
  success. XML counts: 9 tests, 0 skips/failures/errors.
- Read-only psql checks against demo: event titles/statuses/times, request status
  counts, venue count and DB role privileges matched intended fixtures.
- `git check-ignore .local-demo/admin-password.txt .local-demo/run.sh`: both ignored.
- `stat -f '%Sp %N' .local-demo .local-demo/admin-password.txt`: 700 and 600.
- `bash -n .local-demo/run.sh`: syntax passed.

## Verification and limitations

Real service checks confirmed two catalogue results, six Organizer events, admin
login/role/session resolution, authorization for both requests, one conflict and
one non-conflict. Verification sessions were revoked. It did not approve/reject
the pending examples, leaving them available for the user to try.

All nine Attendee tests passed, including three real JDBC tests against the
separate test database. No full-team suite or real JavaFX click-through run in
this task. Warning messages remain warnings, not newly diagnosed product defects.
Attendee venue details, registration, check-in and inbox are still absent.

## Corrections and skill influence

Corrected the installDist classpath after observing the actual directory name.
The skills led to preserving existing databases/config, reusing migrations/auth,
isolating test data, granting a non-superuser DB role and protecting credentials.
No skills changed, no teammate feature was patched, and no dependency was added.

## Outcome

Local demo ready, server left running for interactive use. Launch with
`bash .local-demo/run.sh`; database tests with `bash .local-demo/test.sh`.
Stop only this demo server with `bash .local-demo/stop.sh`. Stop preserves data.
No commit, push or remote changes.

## AI-generated mini reflection

Isolating the data enabled real catalogue database tests without risking existing
work. Explicit synthetic labels avoid presenting fixture publication/approval as
an implemented user workflow. Manual multi-role desktop verification is still
needed and local trust authentication is not production security.

## Student review

- [ ] I confirmed that the original prompt excerpt and terminal-output summary are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
