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

The application opens a home screen. Select **Club Organizer** to create or
edit events, or **Venue Administrator** to open Jordan's venue workspace.

## Club Organizer: create and edit events

1. Select **Club Organizer** on the home screen.
2. Select **New event**, then choose one of the organizer's clubs.
3. Enter a title, optional description, start and end dates, 24-hour Singapore
   times such as `18:00`, and a positive capacity.
4. Select **Save event**. The event is stored as a draft.

The organizer screen currently uses development identity values supplied at
startup (`EVENT_MANAGER_ORGANIZER_ID` and `EVENT_MANAGER_CLUB_IDS`). It stores
event timestamps internally as UTC while displaying Singapore Time (SGT).

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
The Venue Administrator workspace uses its own local-login and database setup;
shared authentication and the organizer-to-venue workflow remain team
integration work.
