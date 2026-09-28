# Event

Event creation, editing, and capacity management belong here. Announcements live
in the `announcement` package.

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

`EventService.publishEvent` moves an owned, not-yet-started draft to `PUBLISHED`
when `EventBookingCheck` confirms a CONFIRMED booking at an ACTIVE venue with the
event's exact times (`storage.JdbcEventBookingCheck` shares the Attendee
registration SQL predicate). Published events cannot be edited; unpublishing is
not supported. After a venue request is approved, `editEvent` only accepts the
booked start/end times, so the booking and the event cannot drift apart. To move
an approved draft, `OrganizerVenueRequestService.releaseApprovedVenue` cancels the
booking and withdraws the request through `VenueRelease` (`storage.JdbcVenueRelease`),
after which the times unlock and a new request can be submitted.

`EventService.deleteEvent` soft-deletes an owned draft (status `DELETED`) through
`DraftEventDeletion` (`storage.JdbcDraftEventDeletion`), which also withdraws a
submitted venue request and releases an approved booking in the same transaction.
`EventService` treats deleted events as not found and hides them from listings.

`JdbcEventRepository` persists each other event mutation (create, edit, publish) and its
sanitized business audit record in one PostgreSQL transaction.
Registration-aware capacity policy remains a future feature. Clubs are still
configured via env IDs (no Clubs CRUD here).
