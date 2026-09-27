package seedu.eventmanager.attendee;

import java.time.Instant;
import java.util.List;
import java.util.Comparator;
import seedu.eventmanager.registration.Registration;

/** Presentation-only filters over authorized owner snapshots; never changes registration rules. */
public final class RegistrationListQuery {
    private RegistrationListQuery() { }

    public enum Filter {
        ALL("All"), UPCOMING("Upcoming"), ONGOING("Ongoing"), PAST("Past"), CANCELLED("Cancelled");
        private final String label;
        Filter(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public enum Order {
        EARLIEST("Earliest first"), LATEST("Latest first");
        private final String label;
        Order(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public static List<MyRegistration> apply(List<MyRegistration> rows, Filter filter, Order order, Instant now) {
        Comparator<MyRegistration> byStart = Comparator.comparing(MyRegistration::startsAt);
        if (order == Order.LATEST) byStart = byStart.reversed();
        return rows.stream().filter(row -> matches(row, filter, now))
                .sorted(byStart.thenComparing(row -> row.eventId().toString())).toList();
    }

    private static boolean matches(MyRegistration row, Filter filter, Instant now) {
        if (filter == Filter.ALL) return true;
        if (filter == Filter.CANCELLED) return row.status() == Registration.Status.CANCELLED;
        if (row.status() == Registration.Status.CANCELLED) return false;
        return switch (filter) {
            case UPCOMING -> now.isBefore(row.startsAt());
            case ONGOING -> !now.isBefore(row.startsAt()) && now.isBefore(row.endsAt());
            case PAST -> !now.isBefore(row.endsAt());
            default -> false;
        };
    }
}
