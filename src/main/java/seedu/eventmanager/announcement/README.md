# Announcement

Organizer event announcements belong here.

## Implemented

`AnnouncementService` lets a Club Organizer, for an event their club owns:

- post an announcement (message required, 1–1000 characters after trimming);
- list the event's announcements, newest first;
- permanently delete one of the event's announcements.

Ownership is checked through `EventService.getEvent`. Announcements cannot be
edited. `JdbcAnnouncementRepository` stores each announcement in
`event_announcement` with a `POST_ANNOUNCEMENT` business audit record in
`event_announcement_audit_record`, in one transaction. Deleting removes the row
and writes a `DELETE_ANNOUNCEMENT` audit record in one transaction; deleting an
announcement that does not exist for that event is rejected as not found with
nothing written. Audit records do not copy the message text.

After the announcement is saved, one `EVENT_ANNOUNCEMENT` notification is
queued through the shared `NotificationService` for each attendee currently
returned by `EventRegistrations`. The payload carries only `announcementId` and
`eventId`. Queueing runs outside the announcement transaction and is
best-effort: a failure for one recipient does not undo the announcement or skip
other recipients, and the result reports queued and failed counts.

## Not implemented / dependencies

- Deleting does not withdraw or cancel notifications already queued for the
  announcement (Organizer owner's decision for this feature).
- Recipients come from `JdbcEventRegistrations`. There is no Attendee
  registration screen or Organizer event publication yet, so in normal use no
  notifications are queued.
- Queued notifications are not delivered: `NotificationOutboxWorker` is not run
  by the application, and the only delivery adapter logs locally.
- Attendees cannot view announcements yet (Attendee "Receive notifications").
