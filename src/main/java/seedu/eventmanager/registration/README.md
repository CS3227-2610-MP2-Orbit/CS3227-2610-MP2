# Registration

Registration records and related business rules belong here.

## Shared contract agreed with Joseph

`EventRegistrations` is the internal, read-only Organizer-facing view.
`registeredAttendees(eventId)` returns `RegisteredAttendee(attendeeId, displayName)`:

- `attendeeId` is `users.user_id`, and `displayName` is `users.username`.
- Include CONFIRMED and CHECKED_IN registrations; exclude CANCELLED.
- Include only active ATTENDEE accounts. Deactivation hides a user from this view
  but does not delete history, cancel registration, or free a reserved place.
- One row per event/account across cancellation and re-registration.
- No ordering guarantee; Organizer consumers sort as needed.
- The query selects only ID and username, never password hashes or session data.
- Default `findRegisteredAttendee` uses exactly the same inclusion rules.

`JdbcEventRegistrations` supplies the implementation. Organizer services must
still authorize event ownership before reading the roster; this internal adapter
is not a public unauthenticated endpoint. Joseph owns replacing his temporary
`NoEventRegistrations` wiring. His interface and record signatures are unchanged.

`EventManagerApplication` now passes `JdbcEventRegistrations` to the Organizer
consumers `VolunteerService` and `event.RegistrationOverviewService`.

See [Registration handoff](../../../../../../docs/RegistrationHandoff.md) for bootstrap,
construction, backend commands, policy, test evidence and remaining integration.
