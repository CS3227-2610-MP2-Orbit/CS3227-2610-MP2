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

The current JavaFX prototype provides:

- A local Venue Administrator login screen.
- A dashboard with sidebar navigation and summary cards.
- A venue-request table with approve and reject actions.
- A venue listing showing name, location, capacity, and status.

Venue CRUD forms, availability management, user access management, and the
organizer map are planned for later iterations.

## Current limitations

The project does not yet include an HTTP server or a production identity
provider. The current JavaFX client uses the local session model and requires
the PostgreSQL database to be available at startup.

Running `gradlew.bat run` now loads the database configuration, applies pending
Flyway migrations, and starts the database-backed Venue Administrator runtime.
It still does not open a visual dashboard. A JavaFX or web view must be
connected to the existing UI controller/state layer before the dashboard can
be viewed interactively.
