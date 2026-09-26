# 021 — Delete announcements (Organizer)

Date: 2026-09-26
Contributor: Joseph (with Cursor agent)
Branch and starting revision: `feature-announcements`; HEAD at completion was the user's commit 6b9e673 ("Merge with main")
Agent/tool: Cursor agent
Skills used (paths and revision or change description):
`.agents/skills/requirements-and-acceptance/SKILL.md`,
`.agents/skills/test-driven-implementation/SKILL.md` (current checkout, unmodified)

## Objective

Let a Club Organizer permanently delete an announcement of an event their club
owns, from the Announcements screen.

## Original prompts (verbatim)

> can you also add a delete announcement feature there as well?

Earlier in the session the user also said:

> can you please stop commiting for me

## Response summary

Added `AnnouncementService.delete`, `AnnouncementRepository.delete` with a JDBC
implementation, a `DELETE_ANNOUNCEMENT` audit action, a **Delete selected**
button with confirmation on the Announcements screen, tests, and docs. Nothing
was committed.

## Assumptions and design decisions

- The earlier "announcements cannot be edited or deleted" rule was the agent's own
  design choice, not an assignment requirement or team decision.
- User decisions (asked before implementation): hard delete (row removed; audit
  keeps actor, time, event ID and announcement ID, not the text); leave
  already-queued notifications untouched.
- Authorization reuses event ownership (`EventService.getEvent`), matching post
  and list. With single-owner clubs this is the club owner.
- An announcement ID that does not belong to the given event is reported as not
  found (`EntityNotFoundException`) with nothing written; deleting twice is not
  found the second time.
- No schema change: the audit table's `action` column is free text and has no
  foreign key to `event_announcement`, so the audit row survives the delete.

Acceptance criteria:

- DEL-01 owner deletes → removed from list, others kept, `DELETE_ANNOUNCEMENT` audit.
- DEL-02 non-owner → `AccessDeniedException`, nothing changes.
- DEL-03 unknown event / unknown announcement / announcement of another event →
  not found, nothing changes.
- DEL-04 second delete → not found.
- DEL-05 no notifications queued, registrations not read.
- DEL-06 (PostgreSQL) delete and audit commit together; audit failure rolls back.

## Files changed

- `src/main/java/seedu/eventmanager/announcement/AnnouncementAuditRecord.java`
- `src/main/java/seedu/eventmanager/announcement/AnnouncementRepository.java`
- `src/main/java/seedu/eventmanager/announcement/AnnouncementService.java`
- `src/main/java/seedu/eventmanager/announcement/JdbcAnnouncementRepository.java`
- `src/main/java/seedu/eventmanager/announcement/README.md`
- `src/main/java/seedu/eventmanager/ui/OrganizerEventView.java`
- `src/test/java/seedu/eventmanager/announcement/AnnouncementServiceTest.java`
- `src/test/java/seedu/eventmanager/announcement/JdbcAnnouncementRepositoryIntegrationTest.java`
- `docs/UserGuide.md`
- `logs/joseph/021-delete-announcements.md` (this log)

Log 020's uncommitted follow-up section is unrelated to this task and was left as is.

## Commands actually executed

Disposable database `event_manager_test_del` created with `createdb`. Dropping it
was blocked by the agent's auto-review; it was left in place for the user to
remove (`dropdb event_manager_test_del`).

```bash
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://localhost:5432/event_manager_test_del EVENT_MANAGER_TEST_DB_USER=josephkwok ./gradlew test --tests 'seedu.eventmanager.announcement.*'
# RED: BUILD FAILED; 9 delete tests failed with UnsupportedOperationException (stubs)
EVENT_MANAGER_TEST_DB_URL=... ./gradlew test --tests 'seedu.eventmanager.announcement.JdbcAnnouncementRepositoryIntegrationTest'
# RED after tightening the rollback test: 3 delete tests failed with UnsupportedOperationException
EVENT_MANAGER_TEST_DB_URL=... ./gradlew test --tests 'seedu.eventmanager.announcement.*'
# GREEN: BUILD SUCCESSFUL; AnnouncementServiceTest 20/20, JdbcAnnouncementRepositoryIntegrationTest 7/7
EVENT_MANAGER_TEST_DB_URL=... ./gradlew classes test --rerun
# BUILD SUCCESSFUL; tests=182 failures=0 errors=0 skipped=2 (PostgreSqlVenueAdministratorIntegrationTest)
```

## Actual verification results

- Red: 7 service tests and 3 integration tests failed on the "not implemented"
  stubs, which is the missing behavior.
- Green: all 10 pass after the implementation; the full suite passes (182).
- Not run: manual desktop check of the Delete selected button and confirmation
  dialog. No automated UI test exists.

## Problems, corrections, and skill revisions

- `delete_auditFailure_rollsBackDelete` initially expected any `RuntimeException`
  and passed vacuously against the stub. It was tightened to
  `EventPersistenceException`, re-run red, then passed after implementation.

## Outcome and limitations

Delete is implemented and verified at service and PostgreSQL level, uncommitted on
`feature-announcements`. Already-queued notifications are not withdrawn. The UI
path is untested.

Suggested commit message:

Allow organizers to delete announcements with audit record

## AI-generated mini reflection

Asking the storage and notification questions first kept the change small and
avoided inventing policy. The strongest evidence is the PostgreSQL test showing the
delete and its audit record commit or roll back together. The main gap is that the
confirmation dialog and button were not exercised; a short manual run is the next
step.

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
