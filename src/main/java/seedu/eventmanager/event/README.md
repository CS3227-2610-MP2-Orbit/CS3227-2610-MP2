# Event

Event creation, editing, capacity management, and announcements belong here.

## Implemented

The Club Organizer can create, list, view, and edit draft events through
`EventService`. Commands validate the title, time interval, and positive
capacity; every workflow checks club ownership. Edits carry an expected version
so a stale editor cannot overwrite a newer change.

`JdbcEventRepository` persists each event mutation and its sanitized business
audit record in one PostgreSQL transaction. Publication, venue approval, and
registration-aware capacity policy remain future features.
