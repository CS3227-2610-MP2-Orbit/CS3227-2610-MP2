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

## Current limitations

The project does not yet include a rendered frontend, HTTP server, concrete API
client, authentication runtime, or PostgreSQL connection configuration.

Running `gradlew.bat run` currently starts the Java entry point and prints a
readiness message. It does not open a visual dashboard. A JavaFX or web view
must be connected to the existing UI controller/state layer before the
dashboard can be viewed interactively.
