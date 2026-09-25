# Registration

Registration records and related business rules belong here.

## Proposed shared contract (pending agreement with the Attendee owner)

`EventRegistrations` is a read-only view of who is registered for an event:

- `registeredAttendees(eventId)` returns `RegisteredAttendee(attendeeId, displayName)`
  for each current registrant;
- `findRegisteredAttendee(eventId, attendeeId)` has a default implementation.

Organizer volunteer assignment (`VolunteerService`) and View registrations
(`event.RegistrationOverviewService`) use it today; event announcements are
expected to use it too. `attendeeId` is intended to be the
shared `users.user_id` of an `ATTENDEE` account.

`NoEventRegistrations` is a placeholder implementation (nobody is registered)
until the Attendee registration feature provides a real one. Registration
storage and rules are not implemented in this checkout.
