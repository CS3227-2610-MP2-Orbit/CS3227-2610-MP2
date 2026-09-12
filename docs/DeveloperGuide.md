# Developer Guide

## Architecture

The project is organized by responsibility. Event, venue, attendee,
registration, notification, and volunteer packages contain domain features.
Shared concerns are separated into common, storage, service, and UI packages.

## Team ownership

- Club Organizer: events, volunteers, and announcements
- Venue Administrator: venue requests, availability, restrictions, conflicts,
  and utilization
- Attendee: event discovery, registration, notifications, check-in, and history

## Development workflow

Run the tests before submitting changes:

```powershell
.\gradlew.bat test
```

Update documentation, tests, and logs whenever behaviour or design changes.

## Acknowledgements

Reused ideas, code, documentation, libraries, and external resources will be
recorded here as the project develops.
