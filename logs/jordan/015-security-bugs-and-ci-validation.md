# 015 — Security bugs, CI validation, and documentation

Contributor: Jordan  
Agent: Codex  
Related records: `005-security-observability-and-errors.md`,
`007-documentation-review.md`, `009-prompt-and-agent-interaction-summary.md`

## Prompt summary

Jordan asked the agent to fix Venue Administrator-related bugs involving
deactivated organizers, deactivated administrators, logout/role switching, and
CI compilation failures. Jordan explicitly left the event publish versus
booking-release race to Joseph.

## Interaction and outcome

The agent added active-account checks to protected Organizer and Venue
Administrator operations, revoked sessions for inactive accounts, and returned
the administrator to shared login after logout. The work was split into focused
commits for capacity/availability, event capacity, deactivated accounts, and
logout behavior.

When CI reported a missing `ApplicationException`, a temporary source-checkout
diagnostic printed the checked-out SHA and source contents. The diagnostic showed
that the CI checkout lacked the import in `JdbcAuthorizationService`. The import
was restored explicitly, and a wildcard-import follow-up was replaced with
explicit imports to satisfy Checkstyle.

The User Guide and Developer Guide were updated to describe capacity limits,
venue deactivation, active-account behavior, and existing-booking policy.

## Verification and limitations

Targeted Java compilation and tests passed in an environment with the Gradle
distribution available. CI failures were investigated from their actual log
output. Full JavaFX end-to-end behavior still requires the project CI or manual
desktop testing.

