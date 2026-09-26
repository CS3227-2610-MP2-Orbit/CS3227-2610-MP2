# 005 — Attendee feature plan

Date: 2026-09-25
Contributor: Johannsen
Branch and starting revision: `attendee`, `bfab6b8956a718930b4f60cf09e7eb16b21f38b3`
Agent/tool: Codex desktop, single agent
Skills used: `.agents/skills/requirements-and-acceptance/SKILL.md` (unchanged)

## Objective

Plan the five assigned Attendee capabilities against current shared contracts,
without implementing unresolved policy or changing application code.

## Original prompts (verbatim)

> looks good, next lets plan out the attendee features

## Response summary

Created `docs/AttendeePlan.md` with source-backed current-state evidence, proposed
policies, sequenced delivery slices, 15 acceptance criteria and test levels,
cross-role dependencies, security/verification limits and single-agent workflow.
Asked for timing/re-registration, QR flow and notification-channel preferences;
the initial document marks these recommendations unaccepted pending the user's
response. Existing evaluation changes and logs were preserved.

## Assumptions and design decisions

- Five capabilities come from the user's role assignment, not an invented rubric.
- Existing synthetic evaluation contracts are not product policy.
- Reuse shared session/audit/outbox/JDBC components. Do not build a second app or
  assume the Organizer and Venue workflows already integrate.
- Prefer small complete slices. Do not claim fixtures prove cross-role integration.
- Recommend before-start registration/cancellation, optional re-registration,
  in-app inbox and attendee-scans-event-QR as proposed choices, not accepted rules.
- Planning only: no application implementation, remote messages, commits or pushes.

## Files changed

- Added `docs/AttendeePlan.md`.
- Added this interaction record.

## Commands actually executed

Working directory: repository root unless stated otherwise. Read-only inspections
used `cat` and `rg` for instructions, skills, source, schema resources, build config,
guides and the log template. Relevant command groups included:

```sh
pwd
git status --short
cat AGENTS.md .agents/skills/requirements-and-acceptance/SKILL.md
rg --files src/main src/test docs | head -180
cat src/main/java/seedu/eventmanager/attendee/README.md src/main/java/seedu/eventmanager/registration/README.md docs/DeveloperGuide.md docs/UserGuide.md
git branch --show-current
git rev-parse HEAD
git diff --no-index --check /dev/null docs/AttendeePlan.md
git diff --no-index --check /dev/null logs/johannsen/005-attendee-feature-plan.md
```

From the parent course directory:

```sh
cat MP2/MP2-spec.md
```

Inspected `Event`, `EventStatus`, `EventRepository`, `EventService`,
`JdbcEventRepository`, `Actor`, `Role`, `AuthorizationService`,
`NotificationService`, `AuditLogService`, `TransactionManager`,
`JdbcTransactionManager`, `JdbcLocalSessionService`, `JdbcNotificationService`,
`LoggingNotificationDelivery`, `NotificationOutboxWorker`, `JdbcDatabase`,
`VenueAdministratorServiceFactory`, `VenueBookingRepository`,
`JdbcVenueBookingRepository`, `EventManagerApplication`, `DatabaseBootstrap`,
`JdbcAuthorizationService`, organizer schema, shared migrations V1–V3 and
`build.gradle`. Paths and relevant symbols are linked in the plan.

## Actual verification results

Read-only inspections completed successfully. The source establishes placeholder
Attendee/registration packages, missing publish/catalogue operations, reusable
local sessions and logging-only notification delivery. Some DeveloperGuide status
text is stale relative to those implementations. This is source inspection, not
runtime verification. No build, product tests, database operation or UI run was
performed for this documentation-only task.
Both new-file diff checks emitted no whitespace diagnostics (exit 1 for differing
files in no-index mode). Final status showed the two planned additions alongside
the preserved pre-existing changes.

## Problems, corrections, and skill revisions

The requirements skill required unresolved policies to remain explicit. It also
prevented the earlier synthetic cancellation exercise from becoming authoritative
product behavior. No skill changes were needed. Source inspection exposed that a
common database URL does not join the organizer's independently opened connection
to the shared transaction boundary; the plan records this integration dependency.

## Outcome and limitations

Partially ready for implementation: policies and cross-role contracts still need
confirmation. The plan describes only the inspected local checkout, not unpulled
teammate work. The local specification was read; Canvas was not independently
revalidated. No application features or verification results are claimed as new.

Suggested commit message: `docs(attendee): plan feature slices and acceptance criteria`

## AI-generated mini reflection

Source-backed planning exposed publication, transaction and notification gaps
before UI implementation. The useful outcome is a small-slice plan with clear
ownership and observable tests. Its limit is unresolved product/team choices;
the next step is to settle those choices and implement the first accepted slice.

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
