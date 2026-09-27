# Attendee

The public read-only event catalogue is implemented here: `EventCatalogueService`
enforces upcoming/published visibility and combines search, exact club ID and
inclusive SGT date filters. `CatalogueEvent` exposes public event fields only.
The JDBC adapter reads the canonical Organizer event table; no duplicate event
store or publication workflow is introduced.

`AttendeeEventDetailsService` resolves the live attendee session and supplies
venue/booking details, remaining seats, own status and eligibility. It uses an
immutable personalized projection, a read-only repository boundary and the
shared registration policy, not the Organizer roster's active-account count.
The JDBC adapter uses a single statement snapshot; the controller handles
asynchronous loading/cancellation and the view only renders results.

Registration commands already exist under `registration`; register/cancel UI,
notifications, check-in and attendance history remain future slices.
See `docs/AttendeePlan.md` for policy and integration dependencies.
