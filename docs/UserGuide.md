# User Guide

## Introduction

Event Venue Manager helps club organizers, venue administrators, and attendees
coordinate events and venues.

Detailed feature instructions will be added as features are implemented.

## Setup

Install JDK 25 and run from the project root:

```powershell
.\gradlew.bat run
```

## Venue Administrator dashboard

The Venue Administrator presentation layer currently defines the dashboard
sections for:

- Pending venue requests
- Upcoming approved bookings
- Venue availability
- Conflict and validation warnings
- Recent decisions and activity

The controller supports loading, empty, success, and error states, and allows
approval or rejection of requests through the API client boundary. Rejections
require a reason. Backend authorization and validation remain authoritative.

The backend persistence foundation includes PostgreSQL repositories, transaction
handling, audit logging, and a notification outbox. The outbox stores approval
and rejection notifications for later delivery; it does not yet send email or
in-app messages.

## Current limitations

The project does not yet include a rendered frontend, HTTP server, concrete API
client, or authentication runtime. PostgreSQL repositories and migrations are
present, but the application entry point is not yet wired to start the
database-backed runtime.

Running `gradlew.bat run` currently starts the Java entry point and prints a
readiness message. It does not open a visual dashboard. A JavaFX or web view
must be connected to the existing UI controller/state layer before the
dashboard can be viewed interactively.
