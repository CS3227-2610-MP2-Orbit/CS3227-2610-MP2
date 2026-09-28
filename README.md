# Event Venue Manager

Event Venue Manager is a Java 25 desktop application for campus events. Club Organizers plan events and request rooms, Venue Administrators decide those requests, and Attendees browse, register, and check in. One shared PostgreSQL database holds the data for all three roles. See the [product website](https://cs3227-2610-mp2-orbit.github.io/CS3227-2610-MP2/) for an overview.

## Quick start

Prerequisites: JDK 25 and PostgreSQL (tested with 16 and 17).

1. Start PostgreSQL and create a database named `event_manager` (for example `createdb event_manager`), then copy `.env.example` to `.env` and fill in your database user.
2. Optional demo data for every role: `./gradlew seedDemo`, or `java -jar <release jar> --seed-demo`. Every demo account uses the password `demo1234`.
3. Run `./gradlew run`, or download `EventVenueManager-1.0.0.jar` from the
   [v1.0.0 release](https://github.com/CS3227-2610-MP2-Orbit/CS3227-2610-MP2/releases/tag/v1.0.0) (one jar for Windows, Linux and Apple Silicon macOS; Intel Macs run from source) and run
   `java -jar EventVenueManager-1.0.0.jar`.

How to install the app and use each role is in the [User Guide](docs/UserGuide.md). Design, user stories, use cases, and non-functional requirements are in the [Developer Guide](docs/DeveloperGuide.md). Those two guides are the description of the product. This file only points to them.

Developer commands:

```sh
./gradlew test
```

A fresh database includes a local Venue Administrator, `admin` / `admin123`, so the administrator workspace can be opened before any other account exists. Setup steps and the warning that this account is for local use only are in the [User Guide](docs/UserGuide.md#getting-started).
