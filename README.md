# Event Venue Manager

Event Venue Manager is a Java 25 desktop application for campus events. Club Organizers plan events and request rooms, Venue Administrators decide those requests, and Attendees browse, register, and check in. One shared PostgreSQL database holds the data for all three roles.

How to install the app and use each role is in the [User Guide](docs/UserGuide.md). Design, user stories, use cases, and non-functional requirements are in the [Developer Guide](docs/DeveloperGuide.md). Those two guides are the description of the product. This file only points to them.

```sh
./gradlew test
./gradlew run
```

A fresh database includes a local Venue Administrator, `admin` / `admin123`, so the administrator workspace can be opened before any other account exists. Setup steps and the warning that this account is for local use only are in the [User Guide](docs/UserGuide.md#getting-started).
