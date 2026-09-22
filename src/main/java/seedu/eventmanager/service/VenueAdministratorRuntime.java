package seedu.eventmanager.service;

import java.util.Objects;
import seedu.eventmanager.storage.DatabaseBootstrap;
import seedu.eventmanager.storage.DatabaseConfiguration;

/** Starts the database-backed Venue Administrator runtime after authentication is supplied. */
public final class VenueAdministratorRuntime {
    private final VenueAdministratorService service;

    private VenueAdministratorRuntime(VenueAdministratorService service) {
        this.service = service;
    }

    public static VenueAdministratorRuntime start(DatabaseConfiguration configuration,
            AuthorizationService authorization) {
        Objects.requireNonNull(configuration);
        Objects.requireNonNull(authorization);
        DatabaseBootstrap.migrate(configuration);
        return new VenueAdministratorRuntime(
                VenueAdministratorServiceFactory.create(configuration, authorization));
    }

    public VenueAdministratorService service() {
        return service;
    }
}
