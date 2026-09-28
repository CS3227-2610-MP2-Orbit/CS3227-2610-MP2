# 008 — Persistent attendee notifications inbox (#30)

Date: 27 September 2026
Contributor: Johannsen
Branch and starting revision: `attendee-registration-ui`, `6189718`; local `attendee` fast-forwarded to that committed baseline before implementation.
Agent/tool: Codex desktop, single agent

## Objective

Implement issue #30 using the existing outbox producers/worker and live sessions:
durable owner-only notifications, read-time text resolution, read/unread commands
and an Attendee Notifications screen. No check-in, email or teammate workflow changes.

## Original prompt

Text preserved below with pasted markup/indentation normalized for readability
(not a byte-for-byte transcript).

> # Task: Issue #30 — Attendee persistent notifications inbox
>
> ## Role
> You are the Attendee developer on the MP2 campus event manager (Java 25, JavaFX,
> PostgreSQL). Work on the `attendee` branch. Follow AGENTS.md guardrails.
>
> ## Goal (from issue #30)
> Let attendees receive and read registration updates and event announcements in
> a persistent notifications inbox with read/unread status.
>
> ## Current state (verify before editing)
> - Producers already queue into the shared `notification_outbox` via
>   `NotificationService.notify(recipientId, event, payload)`, with an idempotency key:
>   - `RegistrationService`: `REGISTRATION_CONFIRMED` / `REGISTRATION_CANCELLED`,
>     payload `registrationId` + `version`, in the same transaction as the state change.
>   - Joseph's `AnnouncementService`: `EVENT_ANNOUNCEMENT`, payload `announcementId`
>     + `eventId`, best-effort outside the announcement transaction.
> - `NotificationOutboxWorker` (claim → deliver → markSent/markFailed, 5 attempts)
>   exists but is NOT run by the application. The only `NotificationDelivery` is
>   `LoggingNotificationDelivery`, so nothing reaches an attendee today.
> - Payloads carry IDs only. Message text must be resolved at read time.
>
> ## Requirements
> 1. Add an inbox persistence model (a new migration in an Attendee-owned stream;
>    do not edit applied migrations) with a unique constraint that makes delivery
>    idempotent per outbox notification.
> 2. Implement a `NotificationDelivery` adapter that writes inbox messages for the
>    three event types above. Unknown event types must fail clearly, not be dropped
>    silently.
> 3. Add a session-scoped inbox service: list own messages (newest first, with an
>    unread count), mark one as read, and mark all as read. Identity always comes from
>    the live session token, never a caller-supplied attendee ID.
> 4. Resolve display text at read time: event title and SGT time for registrations;
>    event title and announcement message for announcements. If the announcement was
>    deleted, show a neutral "announcement removed" entry instead of failing.
> 5. Add a Notifications screen to the Attendee workspace with an unread badge,
>    refresh, mark read and mark all read. Reads run off the FX thread through a
>    controller, following `AttendeeBrowseController`.

## Skills used and influence

Repository skills at baseline `6189718`:

- `.agents/skills/requirements-and-acceptance/SKILL.md`: mapped the five requested
  outcomes to checks and identified worker routing as a shared boundary.
- `.agents/skills/test-driven-implementation/SKILL.md`: observed red assertions
  for unsupported delivery and missing persistence, then implemented and widened checks.
- `.agents/skills/database-migration-and-integrity/SKILL.md`: separate migration
  history, database uniqueness, retry/concurrency and rollback tests.
- `.agents/skills/security-and-rbac/SKILL.md`: live-session ownership on all reads
  and marks, generic other-owner errors, safe exception messages.
- `.agents/skills/desktop-ui-polish/SKILL.md`: existing sidebar shell, background
  controllers, error/empty/loading states and two-size synthetic UI verification.
- `.agents/skills/code-review-and-verification/SKILL.md`: full build before final
  inspection; distinguish PostgreSQL tests, synthetic UI and unrun human E2E.

Also read the installed `supabase:supabase-postgres-best-practices` skill and
`references/lock-short-transactions.md`: short claim/delivery transactions and
an owner/order index. No Supabase product/dependency was introduced.
No skill files were changed by this task.

## Decisions and implementation

- Initial inspection found PR #33 open (`attendee-registration-ui` → `attendee`)
  and PR #25 open (`attendee` → `main`). To follow the requested local branch
  without remote PR mutation, fast-forwarded local attendee to the existing #29
  commit. No push, remote merge or new PR was performed for #30.
- Reused existing producers, session guard, transaction manager and outbox worker.
  No registration or announcement business rules were rewritten.
- Added optional event-type scope to `JdbcNotificationOutboxRepository`; its
  original constructor still claims all types. The attendee dispatcher only
  claims the three supported events. Venue events remain pending for their own
  route. Explicit unsupported delivery throws, rather than pretending success.
- Inbox primary key is the outbox notification ID. Retry/concurrent delivery uses
  conflict-ignore without resetting read status. IDs/type/original notification
  time are stored; event titles/times and announcement text are joined at read time.
- Canonical persisted payloads are used, and registration references must belong
  to the recipient. Inactive ATTENDEE accounts can receive durable messages but
  cannot read/mark without a valid active session.
- Source event/announcement removal does not cascade-delete inbox entries.
  Deleted announcements show "Announcement removed."; missing event details use
  neutral fallback text. Historical notification type is not current booking state.
- List/count come from one immutable result. Mark-one/all filter by session owner,
  run transactionally and revalidate the session. Repeated marks retain read time.
- One app-lifetime daemon dispatcher starts after successful Attendee bootstrap,
  runs bounded batches every two seconds, continues across Home and stops on exit.
  Existing five-attempt/five-minute lease retry semantics are preserved.
- Notifications stays inside the shared Attendee workspace. Reads/marks are off
  FX; duplicate commands and stale/closed read callbacks are guarded. Badge is
  refreshed at initialization, opening/refreshing Notifications and after marks.
  It is not a continuously polled count. Home clears inbox rows.
- No standalone plan file, applied-migration edits, new dependency or demo DB
  mutation was added. Existing unrelated skill-evaluation changes were preserved.

## Files changed

New production files:

- `attendee/InboxDispatcher.java`, `InboxMessage.java`,
  `InboxNotificationDelivery.java`, `InboxRepository.java`,
  `InboxService.java`, `InboxSnapshot.java` under `src/main/java/seedu/eventmanager/`.
- `storage/InboxDatabaseMigration.java` and `storage/JdbcInboxRepository.java`
  under the same source root.
- `ui/InboxActions.java`, `ui/InboxController.java`, `ui/InboxView.java`.
- `src/main/resources/db/attendee_inbox/V1__attendee_notification_inbox.sql`.

Modified production/composition files:

- `src/main/java/seedu/eventmanager/storage/JdbcNotificationOutboxRepository.java`.
- `src/main/java/seedu/eventmanager/ui/AttendeeBrowseView.java`.
- `src/main/java/seedu/eventmanager/ui/EventManagerApplication.java`.

Verification/documentation:

- New `src/test/java/seedu/eventmanager/attendee/InboxNotificationDeliveryTest.java`,
  `InboxServiceTest.java`, `InboxDispatcherTest.java`.
- New `src/test/java/seedu/eventmanager/registration/InboxIntegrationTest.java`.
- New `src/test/java/seedu/eventmanager/ui/AttendeeInboxSmoke.java`.
- Updated existing `AttendeeBrowseSmoke.java`, `AttendeeRegistrationSmoke.java`.
- `build.gradle`, `docs/UserGuide.md`, `docs/DeveloperGuide.md` and this log.

## Commands actually executed and outcomes

Working directory: `CS3227-2610-MP2`.

- `git switch attendee && git merge --ff-only attendee-registration-ui` — exit 0;
  local branch only, baseline now `6189718`.
- `./gradlew test --tests '*InboxNotificationDeliveryTest'` — initial exit 1:
  unsupported-event test expected an exception but passthrough scaffold did not
  throw. Added explicit supported-type guard; subsequent focused runs passed.
- An initial focused PostgreSQL run observed unread count 0 instead of 1 against
  the no-op persistence scaffold. Implemented durable storage; later runs passed.
- `./gradlew classes testClasses` — exit 0 after composition/UI implementation.
- The following focused run passed JUnit, inbox smoke and browse smoke, but exited
  1 in registration smoke because a SplitPane child lookup returned null before
  its first layout pulse:

```sh
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_attendee_test_johannsen EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests '*Inbox*' --tests '*NotificationOutboxWorkerTest' attendeeInboxUiSmoke attendeeUiSmoke attendeeRegistrationUiSmoke
```

- `./gradlew attendeeRegistrationUiSmoke` — exit 0 after waiting for non-null
  list lookup before asserting its unchanged three-row requirement.
- Verified the new disposable database name was absent, then created it:

```sh
/Applications/Postgres.app/Contents/Versions/17/bin/psql -h 127.0.0.1 -p 55448 -U mp2_bootstrap -d postgres -Atc "SELECT datname FROM pg_database WHERE datname='mp2_inbox_verify_20260927'"
/Applications/Postgres.app/Contents/Versions/17/bin/createdb -h 127.0.0.1 -p 55448 -U mp2_bootstrap -O mp2_demo_app mp2_inbox_verify_20260927
```

Both exit 0. No existing database was dropped. Local trust authentication has an
empty test password; no secret is recorded here. Full verification:

```sh
JAVA_TOOL_OPTIONS=-Duser.timezone=UTC DATABASE_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 DATABASE_USER=mp2_demo_app DATABASE_PASSWORD='' DATABASE_INTEGRATION_TESTS=true EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew build --rerun-tasks
./gradlew attendeeInboxUiSmoke attendeeUiSmoke attendeeRegistrationUiSmoke
git diff --check
```

All exit 0. UTC matches the existing suite's offset-sensitive expectations; no
teammate test or application logic was changed for that environment assumption.
JUnit XML counted **233 tests, 0 failures, 0 errors, 0 skipped**. All three
JavaFX smokes passed. Inspected `build/attendee-smoke/inbox-1280.png` and
`inbox-1000.png`: readable sidebar/count, wrapping message rows and usable actions.

## Verification coverage and corrections

- Real PostgreSQL: migration application/re-run, registration producers,
  durable reconnect, duplicate/concurrent delivery, preserved read time,
  owner isolation, inactive/wrong-role/revoked/expired sessions, other-owner
  payload rejection, mark-all scope, rollback, source updates/deletion, malformed
  retry exhaustion and scoped routing.
- Unit: supported/unsupported types, live-session service boundary, immutable
  count/list and daemon dispatcher execution/lifecycle.
- Real JavaFX with synthetic callbacks: badge/count, mark one/all, duplicate
  prevention, background work, loading/empty/error/expiry, stale results and Home.
- Correction: registration smoke dereferenced a child before the SplitPane skin
  exposed it. Changed readiness check, not business assertions or product rules.
  A guessed historical log filename was absent; used `rg --files` to find it.
- No new actionable code-review finding remains from the inspected scope.
  Compilation is the repository's configured static/type check; no dedicated
  CodeQL/dependency security scan or lint plugin was run/configured here.
  No remote CI result or human review is claimed.
- Shared routing change deserves teammate review: default constructor semantics,
  exact supported event set, and venue notifications remaining outside this route.

## Outcome and limitations

Issue #30 implemented locally on attendee, uncommitted/unpushed. Existing remote
PRs are unchanged. Prior #29 commit is now in local attendee; do not casually
push this branch into the existing browse PR.

No full real-login → desktop → database → cross-role manual scenario was executed.
Database integration and synthetic JavaFX checks are separate evidence. The
late-revocation rollback test injects a session resolver; it does not demonstrate
wall-clock expiry during an open PostgreSQL transaction. Shared session resolution
uses the existing transaction-time behavior.

Worker runs only after Attendee initialization while the app is open; pending
messages wait otherwise. UI refresh is explicit, not real-time push. Existing
announcement enqueue remains best effort outside its save transaction; an
announcement that was never queued cannot be recovered by the inbox. Email and
non-attendee delivery are outside this task. Inbox history references outbox rows;
future retention cleanup needs coordination. Test DB is retained for verification,
separate from the user's interactive demo DB.

Suggested commit message: `feat(attendee): add persistent notifications inbox (#30)`

## AI-generated mini reflection

The strongest verified result is the complete outbox-to-inbox storage path with
idempotent delivery and owner-scoped read state. Scoped routing preserved another
role's queue while reusing the existing worker. Synthetic UI checks exposed a
test-readiness race and verified the desktop controls, but are not a replacement
for a real cross-role login demonstration. A useful next step is student testing
of registration and Organizer announcements against the local demo database,
followed by teammate review of the shared routing seam.

## Follow-up — deleted announcements and read-status filters

Original prompt (line wrapping/whitespace normalized):

> for deleted announcements from the club organiser side, deleting an announcement does not withdraw queued notifications base on the readme. Is this accounted for? As well as having a filter in notifications tab where we can filter read or unread

Confirmed the existing Organizer delete path removes only the announcement and
writes its audit entry; it does not withdraw outbox notifications. The inbox
already handles deletion before and after delivery: the durable notification
remains, and the next read renders "Announcement removed." rather than the old
body. Added explicit outbox-retention assertions to the existing real-PostgreSQL
test. This is regression coverage for already-correct behavior, not a new fix to
Joseph's implementation. No Organizer files were changed.

Applied the repository TDD, desktop-UI and review skills, unchanged. Wrote the
filter interaction checks first; `./gradlew attendeeInboxUiSmoke` exited 1 with
the intended assertion `read/unread filter must be available`. Then added the
All/Unread/Read ComboBox in `InboxView`, filtering the already owner-scoped
snapshot. Selection defaults to All and persists across refresh/mark operations.
Filtering preserves newest-first order, clears selection and has a specific
empty-filter message. Marking a row removes it from Unread on refresh. The badge
still counts the entire inbox, and Mark all as read includes hidden messages.
Loading/error/Home clears the cached snapshot so filters cannot restore stale
personal rows. No service, authorization, migration or delivery policy changed.

Changed for this follow-up: `InboxView.java`, `AttendeeInboxSmoke.java`,
`InboxIntegrationTest.java`, User/Developer guides and this log. Existing unrelated
working-tree changes were preserved. No new planning document or remote write.

Green command (exit 0):

```sh
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_attendee_test_johannsen EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests '*Inbox*' attendeeInboxUiSmoke
```

Checks cover all three filters, empty results, global unread badge, filter retained
after marking, read rows leaving Unread, mark-all while unread rows are hidden,
and existing safe error/stale/Home behaviors. UI evidence remains synthetic
callbacks, not database-connected desktop E2E. The PostgreSQL deletion test deletes
source rows directly; the Organizer delete path was inspected, not clicked in the UI.

UI checklist: retained one sidebar and growing content; Show/filter/action labels
fit at both 1280 and 1000 widths; refresh/mark controls remain accessible; shared
colors unchanged; no new Organizer/Admin controls or policies. Inspected the
updated `inbox-1280.png` and `inbox-1000.png` smoke snapshots.

Final broader command (exit 0):

```sh
JAVA_TOOL_OPTIONS=-Duser.timezone=UTC DATABASE_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 DATABASE_USER=mp2_demo_app DATABASE_PASSWORD='' DATABASE_INTEGRATION_TESTS=true EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew build --rerun-tasks attendeeInboxUiSmoke attendeeUiSmoke attendeeRegistrationUiSmoke
```

Full build and all three JavaFX smokes passed; JUnit XML: 233 tests, zero
failures/errors/skips. `git diff --check` passed and final status preserved the
unrelated edits. No new actionable review finding; no security scanner or remote
CI run claimed. Changes remain local and uncommitted on `attendee`.

## Publication follow-up

Original prompt:

> push to git as a new pr please.

Applied the repository review-and-verification skill for publication scope checks.
`git fetch origin` and the open-PR listing confirmed #25 and #33 were still open,
with `origin/attendee-registration-ui` at `6189718`, matching the feature baseline.
Created `attendee-notifications-inbox` using
`git switch -c attendee-notifications-inbox` (exit 0). The new PR targets
`attendee-registration-ui`, stacking on #33 so its diff contains only notifications
work. Existing PR heads are not pushed or merged. After upstream PRs merge, the
base can be adjusted as needed; no existing PR is modified by this publication.

Only inbox production code, related tests, guides, Gradle smoke task and log 008
are selected for the commit. Unrelated `docs/AgenticSE.md`, grader/evaluation
changes and skill-validation artifacts remain local and excluded. No standalone
plan or generated test screenshots are included. Historical "uncommitted" notes
above describe the state before this explicit publication request.

Pre-publication verification reran the full `./gradlew build --rerun-tasks`
command recorded above, with the same UTC and disposable PostgreSQL environment:
exit 0, 233 tests, no failures/errors/skips. The three JavaFX smoke results from
the preceding follow-up still apply; no product code changed during publication.
`git diff --cached --check` passed. Reviewed the explicitly staged 26-file scope;
the unrelated files are absent. No remote CI pass or human approval is claimed.

## Student review

- [x] I confirmed that the original prompts are accurate.
- [x] I confirmed that the changed-file list is accurate.
- [x] I confirmed that recorded commands were actually executed.
- [x] I confirmed that verification results and limitations are accurate.
- [x] I added any mistakes or disagreements omitted by the AI.

Reviewed by: Johannsen Lum
Review date: 29 September 2026
