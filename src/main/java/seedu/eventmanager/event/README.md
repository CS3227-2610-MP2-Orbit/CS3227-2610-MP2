# Event

Event creation, editing, capacity management, and announcements belong here.

## Implemented

The Club Organizer can create, list, view, and edit draft events through
`EventService`. Commands validate the title, time interval, and positive
capacity; every workflow checks club ownership. Edits carry an expected version
so a stale editor cannot overwrite a newer change.

`OrganizerVenueRequestService` submits Jordan-compatible `SUBMITTED` venue
requests for owned events (attendance = event capacity; UTC window from the
event). When `EventService.editEvent` changes capacity, open venue-request
attendance is synced (`DRAFT`/`SUBMITTED` only); decided requests are unchanged.
`RegistrationOverviewService` gives the owning organizer a read-only list of
registrants (sorted by name) with a registered/capacity count, read from
`EventRegistrations`; it writes nothing and enforces no capacity policy.
`OrganizerIds` maps string organizer ids to UUIDs for the venue pipeline until
shared authentication is unified.

`JdbcEventRepository` persists each event mutation and its sanitized business
audit record in one PostgreSQL transaction. Publication and
registration-aware capacity policy remain future features. Clubs are still
configured via env IDs (no Clubs CRUD here).
