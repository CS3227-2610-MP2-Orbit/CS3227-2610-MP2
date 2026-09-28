# CS3227-2610-MP2

Event Venue Manager is a Java 25 desktop application for campus event and venue
management for Club Organizers, Venue Administrators and Attendees. See the
[User Guide](docs/UserGuide.md) for setup and usage, and the
[Developer Guide](docs/DeveloperGuide.md) for design, release and monitoring notes.

## Quick start

1. Start PostgreSQL, then copy `.env.example` to `.env` and fill in your database user.
2. Optional demo data for every role: `./gradlew seedDemo`, or `java -jar <release jar> --seed-demo`.
3. Run `./gradlew run`, or download the jar for your OS from the
   [Releases page](https://github.com/CS3227-2610-MP2-Orbit/CS3227-2610-MP2/releases) and run
   `java -jar <jar>`.

## Development administrator account

The Flyway migration `V5__development_admin_seed.sql` creates an idempotent
local/demo Venue Administrator account so that a fresh development database can
be tested immediately:

```text
Username: admin
Password: admin123
Role: VENUE_ADMINISTRATOR
```

Run the application after configuring the local PostgreSQL connection in `.env`;
Flyway applies the migration automatically. The password is stored as a PBKDF2
hash, and the migration does not overwrite an existing `admin` account.

This credential is for local assessment and demonstrations only. It must be
changed or replaced before any production deployment, and the development seed
must not be used as a production provisioning mechanism.
