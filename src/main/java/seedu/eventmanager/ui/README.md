# UI

The Venue Administrator dashboard is represented by
`VenueAdministratorDashboardController` and
`VenueAdministratorDashboardState`. A concrete JavaFX or web view should bind
to these state objects through `VenueAdministratorApiClient`.

The controller handles role visibility and loading, empty, success, and error
states. Approval and rejection remain API calls; validation and authorization
are authoritative on the backend.
