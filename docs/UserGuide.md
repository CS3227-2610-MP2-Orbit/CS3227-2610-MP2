# User Guide

## Introduction

Event Venue Manager helps club organizers, venue administrators, and attendees
coordinate events and venues.

Detailed feature instructions will be added as features are implemented.

## Setup

Install JDK 25 and PostgreSQL. Create a PostgreSQL database and application user,
then provide the connection through environment variables. Do not put database
credentials in the repository or interaction logs.

```sh
export EVENT_MANAGER_DB_URL='jdbc:postgresql://localhost:5432/event_manager'
export EVENT_MANAGER_DB_USER='event_manager'
export EVENT_MANAGER_DB_PASSWORD='<your local password>'
```

Until shared authentication is integrated, the development adapter uses
`demo-organizer` and `demo-club`. Override those identifiers when needed:

```sh
export EVENT_MANAGER_ORGANIZER_ID='organizer-1'
export EVENT_MANAGER_CLUB_IDS='club-1,club-2'
```

Run from the project root on macOS/Linux:

```sh
./gradlew run
```

Run on Windows:

```powershell
.\gradlew.bat run
```

The application creates its event and business-audit tables if they do not
exist. If PostgreSQL is unavailable, it displays a configuration message without
showing connection credentials.

## Club Organizer: create and edit events

1. Select **New event** and choose one of the organizer's clubs.
2. Enter a title, optional description, UTC start and end timestamps, and a
   positive capacity. Timestamps use ISO-8601 form, such as
   `2026-10-01T10:00:00Z`.
3. Select **Save event**. The event is stored as a draft.
4. Select a draft in **Your events**, change its details, and save again.

The start must precede the end. An organizer can view and edit only events for
clubs supplied by the authenticated-identity contract. Concurrent edits made
from an outdated event version are rejected instead of overwriting newer data.

## Current status

Create/edit events is implemented for the Club Organizer role. Venue requests,
publication, advanced capacity rules, volunteers, registration viewing,
announcements, and shared login are not yet implemented.
