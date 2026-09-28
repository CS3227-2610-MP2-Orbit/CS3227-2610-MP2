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
consumers `VolunteerService`, `event.RegistrationOverviewService` and
`announcement.AnnouncementService`.

Use `RegistrationDatabaseMigration.migrate(configuration)` for explicit schema
bootstrap and `RegistrationServiceFactory.create(configuration, clock)` for
authenticated register/cancel/myRegistrations commands. Registration requires a
future PUBLISHED event, a matching CONFIRMED booking at an ACTIVE venue and a free
seat. Cancellation closes at event start; checked-in registrations cannot cancel.
State changes, audit and outbox writes share one transaction.

See the [Developer Guide](../../../../../../docs/DeveloperGuide.md#team-ownership)
for integration and [log 005](../../../../../../logs/johannsen/005-registration-backend-and-shared-reader.md)
for backend implementation evidence. Registration UI is a separate remaining slice.
