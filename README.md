# Event Venue Manager

Event Venue Manager is a Java 25 desktop application for campus events. Club Organizers plan events and request rooms, Venue Administrators decide those requests, and Attendees browse, register, and check in. One shared PostgreSQL database holds the data for all three roles.

## Quick start

1. Start PostgreSQL, then copy `.env.example` to `.env` and fill in your database user.
2. Optional demo data for every role: `./gradlew seedDemo`, or `java -jar <release jar> --seed-demo`.
3. Run `./gradlew run`, or download the jar from the
   [Releases page](https://github.com/CS3227-2610-MP2-Orbit/CS3227-2610-MP2/releases) (one jar for Windows, Linux and Apple Silicon macOS) and run
   `java -jar <jar>`.

How to install the app and use each role is in the [User Guide](docs/UserGuide.md). Design, user stories, use cases, and non-functional requirements are in the [Developer Guide](docs/DeveloperGuide.md). Those two guides are the description of the product. This file only points to them.

```sh
./gradlew test
./gradlew run
```

A fresh database includes a local Venue Administrator, `admin` / `admin123`, so the administrator workspace can be opened before any other account exists. Setup steps and the warning that this account is for local use only are in the [User Guide](docs/UserGuide.md#getting-started).
