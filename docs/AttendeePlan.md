# Attendee implementation plan

Owner: Johannsen. Inspected: 25 September 2026, `attendee` at `bfab6b8`.
Status: **partially ready — proposed policy and shared integration contracts need
agreement before dependent implementation**. This is a plan, not a feature guide.

### Progress after "go"

The user's follow-up "go" was interpreted as approval to proceed with the
recommended timing/re-registration, self-check-in QR direction and in-app inbox
defaults, starting with the browse slice. It does not settle teammate-owned
publication/booking contracts or prove those integrations exist. The precise QR
window/input method should still be confirmed before that slice.

Implemented locally: public Attendee route, read-only upcoming published catalogue,
literal text search, exact club filter, inclusive SGT start-date filters and
revalidated public details. ATT-01/02 are covered by service/database tests;
ATT-03 covers only existing public fields, not venue/seats/own registration.
ATT-15 is exercised for this browse view only. No authenticated mutations exist.
The current shared publication workflow remains missing, so cross-role slice-1
integration is pending. See User/Developer Guides and log 006 for actual checks.

## Scope and authority

The user assigned five Attendee capabilities: browse/search events, register/cancel,
receive notifications, QR-code check-in, and attendance history. Deliver these in
one Java desktop application using shared services, not a separate application.

The local `../MP2/MP2-spec.md` (outside this repository) specifies Java 25, separate
role UIs, shared components, production-level engineering and a customized single
agent. It does not prescribe cancellation, registration or check-in policy.
The prior `evals/` cancellation contracts are synthetic: none becomes product
policy through this plan. No live remote branch or Canvas update was checked.

## Current evidence and integration gaps

| Source | Observed behavior | Planning consequence |
| --- | --- | --- |
| [attendee](../src/main/java/seedu/eventmanager/attendee/README.md), [registration](../src/main/java/seedu/eventmanager/registration/README.md) | README placeholders, no feature classes | Build the role workflow and registration domain |
| [EventService](../src/main/java/seedu/eventmanager/event/EventService.java), [EventStatus](../src/main/java/seedu/eventmanager/event/EventStatus.java) | Creates/edits drafts; PUBLISHED and COMPLETED enum values exist, but no publish operation | Joseph owns publication; do not make drafts public as a workaround |
| [EventRepository](../src/main/java/seedu/eventmanager/event/EventRepository.java) | Organizer-scoped listing, no attendee catalogue | Agree a public event read boundary without bypassing organizer authorization |
| [VenueBookingRepository](../src/main/java/seedu/eventmanager/service/VenueBookingRepository.java) | Conflict checks and booking creation, no event-to-booking read contract | Jordan supplies confirmed venue details/eligibility integration |
| [Actor](../src/main/java/seedu/eventmanager/common/Actor.java), [sessions](../src/main/java/seedu/eventmanager/storage/JdbcLocalSessionService.java) | UUID users, ATTENDEE role and local session resolution exist | Reuse session storage; add attendee routing and ownership enforcement, not another identity model |
| [JdbcEventRepository](../src/main/java/seedu/eventmanager/event/JdbcEventRepository.java), [JdbcDatabase](../src/main/java/seedu/eventmanager/storage/JdbcDatabase.java) | Organizer opens independent connections; shared JDBC uses an instance-local transaction connection | A common database URL alone does not make operations atomic; agree transaction-aware adapters |
| [notification service](../src/main/java/seedu/eventmanager/storage/JdbcNotificationService.java), [worker](../src/main/java/seedu/eventmanager/notification/NotificationOutboxWorker.java), [delivery](../src/main/java/seedu/eventmanager/notification/LoggingNotificationDelivery.java) | Durable outbox and retry worker exist; delivery adapter logs only | Add a persistent owner-scoped inbox delivery adapter; an outbox row or log is not a received message |
| [home screen](../src/main/java/seedu/eventmanager/ui/EventManagerApplication.java) | Only Organizer and Venue Administrator routing | Add Attendee entry and reuse the visual conventions |

Some implementation-status paragraphs in DeveloperGuide lag the code (sessions
and a notification worker already exist). Use source evidence above for planning;
update affected guide sections when integrating, without claiming unverified UI.

## Policy ledger

P2–P4 follow the recommended defaults under the announced interpretation of
"go"; P5 has only a selected direction, with window/input confirmation pending.
P1's read-only visibility/filtering is implemented; publication/booking eligibility
still needs the team handoff. P6/P7 remain proposed contracts for later slices.
No future capability below is described as implemented merely because its policy
has been discussed or accepted.

| ID | Recommendation | Criteria / decision owner |
| --- | --- | --- |
| P1 | Catalogue shows upcoming PUBLISHED events; only appropriately confirmed venue bookings make an event registerable. No draft leakage. Search title/description, filter date/club, sort start then stable ID. Venue label comes from shared booking data. | ATT-01–03; Johannsen + Joseph + Jordan |
| P2 | Authenticated ATTENDEE registers only themself, strictly before start, while capacity remains. Duplicate active registration is a no-op with an explicit result. No waitlist. | ATT-04–06; user confirmation |
| P3 | Owner may cancel a CONFIRMED registration strictly before start; equality/later and CHECKED_IN reject. CANCELLED retries are no-ops after authorization. Re-registration is allowed if currently eligible and a seat remains. | ATT-07–08; user confirmation |
| P4 | Confirmations, cancellation confirmations and organizer announcements appear in an in-app inbox; email/SMS/push and scheduled reminders are deferred. | ATT-09–10; user confirmation; announcement producer with Joseph |
| P5 | Attendee scans an event QR and checks in themself; proposed window is `[start, end)`. Reuse authenticated identity; never accept an attendee ID supplied by the QR. Initial desktop input: decode a QR image chosen by the user; webcam support deferred. | ATT-11–13; user chooses flow, then confirms window/input |
| P6 | Attendance history is the owner's checked-in events, retained after completion. Keep cancelled registrations visible in My Registrations, not counted as attendance. No inferred absence penalties or attendance percentages. | ATT-14; user confirmation |
| P7 | Each successful registration/cancellation/check-in state transition and its audit/outbox effects commit atomically. Retries produce no duplicate effects. Notification delivery is asynchronous and idempotent. | ATT-06–13; proposed engineering contract, shared-service agreement |

If personal QR tickets scanned by an organizer are preferred, replace P5 before
implementation: attendee displays their ticket, an event-authorized organizer
scans it, and the check-in service enforces scanner scope. That adds a cross-role
screen and ownership contract. Do not quietly build both flows.

Self-check-in establishes an authenticated claim, not proof of physical presence:
an event QR/image can be forwarded. Confirm this limitation is acceptable. QR
validity, event binding, expiry, cancelled registration and replay must still be
checked by the application service. Do not log or place raw QR secrets in audit
records or notification payloads. QR dependency selection is deferred to the QR
slice and must be verified for Java/JavaFX packaging then.

All time comparisons use an injected authoritative UTC clock; the UI uses existing
SGT formatting. Authentication comes from resolving a live session at the command
boundary, not a role dropdown or arbitrary constructed Actor. Local desktop/JDBC
is the existing trust model; client checks alone do not secure a publicly exposed
database. Do not claim hardened public deployment without team security review.

## Delivery slices

| Order | Deliverable | Exit condition / dependency |
| --- | --- | --- |
| 0 | Agree policies and shared contracts | Confirm P1–P7 as applicable; agree event IDs/publication, booking lookup, session boundary and transaction strategy with teammates. No remote changes or messages implied by this plan. |
| 1 | Browse/search + event details + Attendee shell | ATT-01–03, ATT-15; real read adapter and verified published source. Isolated fixtures can support unit/UI development while publication is unavailable, but do not count as team integration. |
| 2 | Register + My Registrations | ATT-04–06; persistent records, duplicate protection, last-seat concurrency, audit/outbox. First substantial TDD demonstration. |
| 3 | Cancel + re-register | ATT-07–08; cutoff boundaries, ownership, retry semantics and restored availability. Registration lifecycle version distinguishes genuinely new notifications from retries. |
| 4 | In-app inbox | ATT-09–10; transactional outbox feeds persistent inbox; wire existing worker lifecycle and retry handling. Coordinate announcement production separately. |
| 5 | QR check-in + attendance history | ATT-11–14; real QR decoding/generation path, time/ownership validation, replay protection and persisted history. |
| 6 | Cross-role verification and release integration | Real organizer→venue approval→publication→registration→notification→check-in→history demo, regression tests, accurate docs, packaging and monitoring evidence. |

Each slice includes domain/service behavior, persistence, minimal UI, tests and
its log. Avoid implementing every backend first and postponing all integration.
All five assigned capabilities remain in scope; deferrals above concern extras.

## Acceptance criteria (conditional on the policies above)

| ID | Given / When / Then | Verification |
| --- | --- | --- |
| ATT-01 | Given mixed draft/published/completed events, when an attendee lists or opens an event directly, then only the agreed visible records are exposed; drafts remain inaccessible. | Unit + repository/service integration |
| ATT-02 | Given visible events, when search/date/club filters are combined, then results match the agreed predicates, have deterministic ordering and a clear empty state. Proposed date range is inclusive SGT calendar days converted to UTC boundaries. | Unit boundaries + database + UI |
| ATT-03 | Given event details and current booking/capacity, when displayed, then title, description, club, SGT times, venue, availability and own registration status agree with stored data; a stale enabled button cannot bypass revalidation. | Service + JavaFX workflow |
| ATT-04 | Given an authenticated attendee and an eligible event with a seat, when registration succeeds, then exactly one active own registration persists and appears after restart; wrong role/expired session cannot mutate it. | Unit + real database + restart smoke test |
| ATT-05 | Given a full, unpublished or started event, or an existing active registration, when registering, then the defined rejection/no-op leaves registrations and effects unchanged. | Unit partition/boundary tests + integration |
| ATT-06 | Given concurrent duplicate calls or two attendees competing for the last seat, when executed on independent connections, then uniqueness and capacity hold. Given a required state/audit/outbox write failure, then all transition writes roll back. | Real PostgreSQL concurrency + fault injection, not mocks alone |
| ATT-07 | Given the owner and CONFIRMED state, when cancellation occurs just before/start/after the cutoff, then P3 governs the result. A success frees one place and persists one audit/outbox effect; another user cannot cancel or read the private record. | Controlled-clock unit + database integration |
| ATT-08 | Given a cancelled registration, when an authorized cancellation retry occurs, then no additional effects occur. When re-registering successfully under P2/P3, then one seat is consumed and a new lifecycle transition is notified; old retries must not silently undo a later registration. Use expected lifecycle/version to reject stale commands. | Unit lifecycle + concurrent database tests |
| ATT-09 | Given a committed notification event, when delivery runs/retries, then one inbox message exists for its recipient. Failures remain retryable without undoing a committed registration; own messages survive restart and read status persists. | Worker/adapter integration + UI |
| ATT-10 | Given another recipient's message ID or an announcement sent to a defined event audience, when accessing/delivering it, then ownership and agreed audience are enforced. Announcements need Joseph's producer/audience contract; do not claim working integration from seeded messages. | Authorization + cross-role integration |
| ATT-11 | Given a valid event QR, owner session and active registration inside the accepted window, when decoded and submitted, then the event-bound registration becomes CHECKED_IN with timestamp and audit, visible after restart. | Decoder/unit + service/database + real UI path |
| ATT-12 | Given malformed, forged/unknown, wrong-event, expired/out-of-window QR input, a non-registered/cancelled attendee or a disallowed actor, when check-in is attempted, then no attendance or success side effects are recorded. | Negative/security-focused unit + integration |
| ATT-13 | Given a successful check-in, when duplicated or raced, then exactly one transition/timestamp and one set of effects exist. CHECKED_IN cannot cancel. Invalid/replayed input never substitutes another attendee's identity. | State-machine + database concurrency |
| ATT-14 | Given checked-in events across time, when viewing history, then only the owner's actual attendance appears with event/date/venue/check-in time and persists after completion/restart. Cancelled/unattended registrations are not fabricated attendance. | Query integration + UI |
| ATT-15 | Given slow loads, empty data, validation rejection or storage failure, when using each screen, then loading/empty/error/retry states are clear, duplicate submissions are guarded, the UI remains responsive and no false success or raw secrets appear. | Controller tests + actual JavaFX smoke checks |

These criteria are proposed observable contracts, not reports of executed tests.
Result codes and ambiguous record-existence disclosure should be agreed with the
shared error mapping before implementation. Authorized retry handling must not
bypass authentication/ownership or override a newer lifecycle version.

## Design and team handoffs

- Keep orchestration in `attendee`, registration state/invariants in `registration`,
  JDBC adapters in `storage`, and views/controllers in `ui`. Suggested screens:
  Browse Events (details), My Registrations, Notifications, Check-in, Attendance
  History; one sidebar with Home and Logout. Avoid a second nested shell.
- Reuse `Actor`, `Role`, session resolution, `AuditLogService`, `NotificationService`,
  `TransactionManager`, error mapping, structured logging and SGT formatting.
  Registration ownership is a new workflow check, not supplied by venue scope checks.
- Keep the Organizer event ID as the common reference; do not introduce duplicate
  attendee event storage or rewrite organizer authorization to expose a catalogue.
  Proposed read boundary: event summary/details plus booking-derived venue status.
- Registration transitions: none→CONFIRMED; CONFIRMED→CANCELLED or CHECKED_IN;
  CANCELLED→CONFIRMED only under P3. Preserve transition history/version; completion
  belongs to the event, not a fabricated attendance transition.
- For capacity, choose one authoritative calculation/reservation mechanism and one
  shared locking protocol. A read-count-then-insert check outside a transaction is
  insufficient. Event publication/capacity/booking changes must participate in the
  agreed protocol or reject incompatible updates; do not use stale UI availability.
- Use the same `JdbcDatabase` instance for each atomic registration/audit/outbox
  operation. Existing organizer repository operations do not join that transaction
  automatically. Coordinate migration order and ownership; do not edit applied
  migrations or reserve a migration number without checking teammates' changes.
- Inbox delivery must deduplicate by notification ID. Existing outbox deduplication
  uses event + recipient + payload, so include a stable transition identity: a retry
  reuses it; a new re-registration does not. A SENT outbox row is not an inbox record.
- **Joseph:** publication eligibility, public event reads, capacity-change rules,
  announcement audience/events, QR event distribution or scanner (depending on P5).
- **Jordan:** confirmed booking lookup, venue availability/status semantics, shared
  sessions/composition/migrations and what happens if an approved booking becomes
  AT_RISK/cancelled. Registration reopening and attendee notices need an explicit
  cross-role contract, not silently inferred event-cancellation support.

## Agentic SE and completion evidence

For each slice, use one agent sequentially: requirements skill → review/accept
criteria → TDD skill (observed behavioral red, minimal green, refactor) → review
skill (scoped diff, checks, findings/limits). Use desktop-ui-polish only when doing
that UI work. Retain actual prompts, failures, corrections and command results;
student reflections/review are written or checked personally.

Three strong real-work demonstrations: requirements clarifies QR/cancellation
ownership; TDD exposes duplicate/last-seat behavior; review catches a non-owner
access path or verifies rollback against evidence. Do not plant an application
defect or invent a finding just to demonstrate a skill. Experiments remain separate
from product tests, and no skill improvement is claimed without comparative evidence.

Definition of done: criteria linked to tests; live database concurrency/rollback
evidence; actual desktop happy/rejected paths; no skipped test counted as a pass;
safe diagnostic outcomes and business audits kept separate; CI updated for new
checks; current User/Developer Guides; reviewed interaction log; team integration
and packaged JavaFX smoke test. No commits, pushes, PRs or release changes are
authorized by this planning document alone.

Next action after browsing: agree publication/booking and shared transaction
handoffs before implementing registration. Finalize the QR window/input before
the QR slice. Keep synthetic UI fixtures distinct from real published events.
