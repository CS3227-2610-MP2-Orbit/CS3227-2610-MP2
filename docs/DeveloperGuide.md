# Developer Guide

## Architecture

The project is organized by responsibility. Event, venue, attendee,
registration, notification, and volunteer packages contain domain features.
Shared concerns are separated into common, storage, service, and UI packages.

## Team ownership

- Club Organizer: events, volunteers, and announcements
- Venue Administrator: venue requests, availability, restrictions, conflicts,
  and utilization
- Attendee: event discovery, registration, notifications, check-in, and history

## Development workflow

Run the tests before submitting changes:

```powershell
.\gradlew.bat test
```

Update documentation, tests, and logs whenever behaviour or design changes.

### Agentic SE workflow

Project-wide agent instructions are in [`AGENTS.md`](../AGENTS.md). Three shared
SWE skills live under `.agents/skills/`: requirements and acceptance criteria,
test-driven implementation, and code review and verification. These apply across
all three roles. See [Agentic SE](AgenticSE.md) for invocation examples, validation
scenarios, and the distinction between structural and behavioral validation.

Record meaningful interactions under `logs/<contributor>/` using the
[interaction template](../logs/templates/interaction.md). The student personally
reviews the evidence before checking the review boxes. Existing summaries remain
available in `logs/prompts_summary.md`.

## Acknowledgements

Reused ideas, code, documentation, libraries, and external resources will be
recorded here as the project develops.

The initial SWE skill structure was developed with Codex using its bundled
`skill-creator` guidance. The interaction-log format adapts Johannsen's MP1
development-record practice; no MP1 application code was reused for this change.
