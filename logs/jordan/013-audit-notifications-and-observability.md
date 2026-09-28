# 013 — Audit, notifications, and observability

Contributor: Jordan  
Agent: Codex  
Related record: `005-security-observability-and-errors.md`

## Prompt summary

Jordan asked whether the logger recorded database changes, clarified the
difference between audit records and diagnostic logging, and asked what the
notification outbox and monitoring represented in the application.

## Interaction and outcome

The agent clarified that business audit records record important decisions such
as who approved or rejected a request. Diagnostic logs record operational
events, failures, and metrics; they are not a complete history of every database
mutation. The notification outbox is a backend delivery mechanism rather than
a separate Venue Administrator inbox.

Venue decisions use shared audit, notification, structured logging, metrics,
and transaction services. Notification failure is logged and measured without
undoing a successfully committed decision.

## Verification

Error-mapping, authorization, and service tests were reviewed. Sensitive values
such as passwords, tokens, and hashes were excluded from diagnostic logging.

