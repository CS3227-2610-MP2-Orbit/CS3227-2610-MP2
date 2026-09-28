# Attendee

The public read-only event catalogue is implemented here: `EventCatalogueService`
enforces upcoming/ongoing published visibility and combines search, exact club ID and
inclusive SGT date filters. `CatalogueEvent` exposes public event fields only.
The JDBC adapter reads the canonical Organizer event table; no duplicate event
store or publication workflow is introduced.

`AttendeeEventDetailsService` resolves the live attendee session and supplies
venue/booking details, remaining seats, own status and eligibility. It uses an
immutable personalized projection, a read-only repository boundary and the
shared registration policy, not the Organizer roster's active-account count.
The JDBC adapter uses a single statement snapshot; the controller handles
asynchronous loading/cancellation and the view only renders results.

Registration commands live under `registration`. Register/cancel, My Registrations,
persistent notifications and normal self-check-in are implemented. Check-in uses
the shared deterministic policy in the existing registration service and permits
only an eligible own confirmed registration from start (inclusive) to end
(exclusive). Ongoing events never offer Register/Re-register. A separate
attendance-history screen remains a future slice.
See the [Developer Guide](../../../../../../docs/DeveloperGuide.md#attendee-catalogue-and-personalized-event-details)
for implemented behavior and integration boundaries.
