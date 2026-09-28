package seedu.eventmanager;

import javafx.application.Application;
import seedu.eventmanager.demo.DemoDataSeeder;
import seedu.eventmanager.ui.EventManagerApplication;

/** Starts the Event Venue Manager application. */
public final class Main {
    public static final String VERSION = "0.1.0";

    private Main() {
    }

    /** Starts the application. */
    public static void main(String[] args) {
        if (args.length == 1 && "--version".equals(args[0])) {
            System.out.println("Event Venue Manager " + VERSION);
            return;
        }
        if (args.length == 1 && "--seed-demo".equals(args[0])) {
            DemoDataSeeder.main(new String[0]);
            return;
        }
        Application.launch(EventManagerApplication.class, args);
    }
}
