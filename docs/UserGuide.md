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

## Club Organizer: request a venue

1. In **Venue Administrator**, create at least one **ACTIVE** venue under
   **Venues** (creating a venue also grants you access to approve requests for
   it). For older venues, select the row and use **Claim access**.
2. Create or select an owned event under **Events** / **New event**.
3. Open **Request venue** in the sidebar.
4. Select the event and an **ACTIVE** venue from the list.
5. Select **Submit request**. The request is stored as `SUBMITTED` using the
   event's schedule and capacity as expected attendance.
6. Open **Venue Administrator** → **Venue requests** (the list reloads when you
   open that screen) and approve or reject the pending request.

Organizer identity is still development-configured (`EVENT_MANAGER_ORGANIZER_ID`).
Non-UUID organizer ids are mapped to a stable UUID for the venue pipeline
(temporary until shared auth is unified). An event may have only one open
(`DRAFT`/`SUBMITTED`) venue request at a time. Booking conflicts are still
checked when the Venue Administrator approves, not at submit time.

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
- Creating a venue grants the logged-in administrator access to that venue;
  **Claim access** covers venues created earlier.
- **Venue requests** reloads pending `SUBMITTED` rows each time you open it.

Venue availability management, user access management, and the organizer map
continue to evolve; shared authentication with Club Organizer remains future
work.

## Current limitations

Organizer can submit venue requests into the shared `venue_requests` table so
Venue Administrators can approve or reject them. Shared authentication is not
unified yet: Organizer still uses env identity, while Venue Admin uses local
login. Organizer string ids are mapped to UUIDs for `organizer_id` (name-based
when not already a UUID). Supersede/withdraw and submit-time conflict checks
are not implemented. Clubs are still configured via `EVENT_MANAGER_CLUB_IDS`
(no Clubs CRUD UI). The Venue Administrator workspace accepts the same
`EVENT_MANAGER_DB_URL`, `EVENT_MANAGER_DB_USER`, and optional
`EVENT_MANAGER_DB_PASSWORD` settings as the organizer workspace.
