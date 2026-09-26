# Attendee

The public read-only event catalogue is implemented here: `EventCatalogueService`
enforces upcoming/published visibility and combines search, exact club ID and
inclusive SGT date filters. `CatalogueEvent` exposes public event fields only.
The JDBC adapter reads the canonical Organizer event table; no duplicate event
store or publication workflow is introduced.

Registration, notifications, check-in and attendance history remain future slices.
See `docs/AttendeePlan.md` for policy and integration dependencies.
