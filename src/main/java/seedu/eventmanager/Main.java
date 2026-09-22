package seedu.eventmanager;

import seedu.eventmanager.service.VenueAdministratorRuntime;
import seedu.eventmanager.storage.DatabaseBootstrap;
import seedu.eventmanager.storage.DatabaseConfiguration;
import seedu.eventmanager.storage.JdbcAuthorizationService;
import seedu.eventmanager.storage.JdbcDatabase;

/** Starts the Event Venue Manager application. */
public class Main {
    /** Starts the application. */
    public static void main(String[] args) {
        DatabaseConfiguration configuration = DatabaseBootstrap.configuration();
        JdbcDatabase database = new JdbcDatabase(configuration);
        VenueAdministratorRuntime.start(configuration, new JdbcAuthorizationService(database));
        System.out.println("Event Venue Manager backend is ready.");
    }
}
