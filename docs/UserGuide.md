# User Guide

## Introduction

Event Venue Manager helps club organizers, venue administrators, and attendees
coordinate events and venues.

Detailed feature instructions will be added as features are implemented.

## Setup

Install JDK 25 and a local PostgreSQL server (for example Postgres.app on macOS).
Create a project-root `.env` (gitignored) with your database settings, for example:

```env
DATABASE_URL=jdbc:postgresql://localhost:5432/event_manager
DATABASE_USER=your_postgres_username
EVENT_MANAGER_DB_URL=jdbc:postgresql://localhost:5432/event_manager
EVENT_MANAGER_DB_USER=your_postgres_username
```

Create the `event_manager` database once if it does not exist. Then run from the
project root:

```powershell
.\gradlew.bat run
```

```sh
./gradlew run
```

The application opens a home screen. Select **Club Organizer** to create or
edit events, or **Venue Administrator** to open Jordan's venue workspace.
Restart the app after changing `.env`.

## Club Organizer: create and edit events

1. Select **Club Organizer** on the home screen.
2. Use **New event** in the left sidebar to start a blank draft (default times
   `18:00` / `20:00` and capacity `80`). Use **← Home** in the same sidebar to
   return to the role picker.
3. Select **New event**, then choose one of the organizer's clubs.
4. Enter a title, optional description, start and end dates, 24-hour Singapore
   times such as `18:00`, and a positive capacity.
5. Select **Save event**. The event is stored as a draft.

The organizer screen currently uses development identity values supplied at
startup (`EVENT_MANAGER_ORGANIZER_ID` and `EVENT_MANAGER_CLUB_IDS`). Clubs are
not created through a UI button yet: set `EVENT_MANAGER_CLUB_IDS` in `.env` to a
comma-separated list (for example `demo-club,chess-club`), then restart the app.
It stores event timestamps internally as UTC while displaying Singapore Time
(SGT).

On the events screen, **Reset** clears a new draft form. While editing a saved
event, the button becomes **Revert changes** and reloads the last saved draft
from the database (discarding unsaved edits). The Organizer workspace uses a
single sidebar chrome (no second outer Home bar) so the form can use full width.

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

The role home screen only routes between the two existing role workspaces. It
does not yet create venue requests from organizer events, so a venue
administrator cannot approve an event created through the organizer screen.
The Venue Administrator workspace accepts the same `EVENT_MANAGER_DB_URL`,
`EVENT_MANAGER_DB_USER`, and optional `EVENT_MANAGER_DB_PASSWORD` settings as
the organizer workspace. It still uses its own local-login model; shared
authentication and the organizer-to-venue workflow remain team integration
work.
