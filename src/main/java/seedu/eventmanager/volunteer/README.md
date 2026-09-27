# Volunteer

Volunteer assignment and management belong here.

## Implemented

`VolunteerService` lets a Club Organizer, for an event their club owns:

- assign a **registered attendee** as a volunteer, with an optional role label
  (up to 60 characters, e.g. "Usher");
- remove a volunteer;
- list volunteers (an attendee who is no longer registered is shown without a name);
- list registered attendees not yet assigned.

Ownership is checked through `EventService.getEvent`. Duplicate assignments are
rejected. `JdbcVolunteerRepository` stores assignments in `event_volunteer` and
writes an `ASSIGN_VOLUNTEER` / `REMOVE_VOLUNTEER` business audit record in
`event_volunteer_audit_record` in the same transaction. Attendee names are not
stored; they are read from `EventRegistrations` at list time.

The Organizer **Volunteers** screen (`OrganizerEventView`) calls this service;
`EventManagerApplication` wires it with `JdbcEventRegistrations`.

## Not implemented / unresolved

- There is no Attendee registration screen or Organizer event publication yet,
  so in normal use the Volunteers screen has no attendees to assign.
- Undecided team policy: what happens to an assignment when the attendee cancels
  their registration, volunteer caps, volunteer notifications, attendee sign-up,
  and whether volunteers may perform QR check-in.
