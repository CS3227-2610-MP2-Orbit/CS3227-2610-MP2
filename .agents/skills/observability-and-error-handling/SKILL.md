---
name: observability-and-error-handling
description: Review and implement safe structured logging, consistent errors, audit events, notifications, and supported metrics for production workflows.
---

# Observability and error handling

Use this skill for service failures, API errors, audit logging, notification
flows, monitoring, and production-readiness reviews.

## Inspect first

Trace the existing logging, error-response, audit, notification, correlation-ID,
and metrics abstractions. Reuse them instead of adding parallel infrastructure.

## Logging and errors

- Log important business outcomes and unexpected failures with useful context.
- Include action, result, resource identifiers, and correlation context when
  available.
- Never log passwords, authentication tokens, password hashes, or unnecessary
  personal information.
- Return stable user-facing errors and keep internal exception details in logs.
- Distinguish validation, authorization, not-found, conflict, dependency, and
  unexpected failures.

## Consistency and verification

Check transaction boundaries, notification failure behavior, audit requirements,
and UI loading/error states. Add only metrics supported by the existing
monitoring setup. Test both successful outcomes and prohibited side effects on
failure.
