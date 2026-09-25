---
name: security-and-rbac
description: Review and implement authentication, role-based access control, and object-level authorization without duplicating the project's existing security system.
---

# Security and RBAC

Use this skill for security-sensitive changes involving login, roles, protected
routes, administrator actions, or resource ownership.

## Inspect first

Read the repository instructions and trace the existing authentication,
session, role, route, and error-handling code. Reuse existing security
abstractions. Do not create a second authentication or authorization system.

## Review rules

- Require authentication at the backend boundary.
- Enforce role permissions on the backend, independently of UI visibility.
- Check object ownership or permitted scope for resource access.
- Test unauthenticated, forbidden-role, unauthorized-resource, and valid cases.
- Do not expose passwords, tokens, password hashes, or unnecessary personal data.
- Return consistent safe errors without leaking implementation details.

## Verify

Inspect direct API behavior, protected frontend routes, session handling, and
security-relevant tests. Record the commands and distinguish executed checks
from source-only review.
