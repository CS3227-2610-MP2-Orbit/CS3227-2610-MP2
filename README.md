# CS3227-2610-MP2

Event Venue Manager is a Java 25 desktop application for campus event and venue
management. The current implementation includes the Club Organizer's create and
edit event workflow. See the [User Guide](docs/UserGuide.md) for setup and usage.

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
