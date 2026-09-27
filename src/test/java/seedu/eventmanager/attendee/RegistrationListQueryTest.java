package seedu.eventmanager.attendee;

import static org.junit.jupiter.api.Assertions.*;
import static seedu.eventmanager.attendee.RegistrationListQuery.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.registration.Registration;

class RegistrationListQueryTest {
    private final Instant now = Instant.parse("2030-01-01T00:00:00Z");
    private final MyRegistration upcoming = row(1, 1, 3600, Registration.Status.CONFIRMED);
    private final MyRegistration ongoing = row(2, 0, 3600, Registration.Status.CONFIRMED);
    private final MyRegistration past = row(3, -3600, 0, Registration.Status.CHECKED_IN);
    private final MyRegistration cancelled = row(4, -3600, 3600, Registration.Status.CANCELLED);
    private final List<MyRegistration> rows = List.of(upcoming, ongoing, past, cancelled);

    private MyRegistration row(long id, long start, long end, Registration.Status status) {
        return new MyRegistration(new UUID(0, id), "Event " + id, now.plusSeconds(start), "Room",
                status, 2, false, now.plusSeconds(end), "club", "Description", "PUBLISHED");
    }

    @Test void classifiesExactStartAndEndWithoutMixingCancelledIntoTimeGroups() {
        assertEquals(List.of(upcoming), apply(rows, Filter.UPCOMING, Order.EARLIEST, now));
        assertEquals(List.of(ongoing), apply(rows, Filter.ONGOING, Order.EARLIEST, now));
        assertEquals(List.of(past), apply(rows, Filter.PAST, Order.EARLIEST, now));
        assertEquals(List.of(cancelled), apply(rows, Filter.CANCELLED, Order.EARLIEST, now));
    }

    @Test void allIncludesEveryStatusAndSortsByStartWithStableIdTieBreaker() {
        assertEquals(List.of(past, cancelled, ongoing, upcoming), apply(rows, Filter.ALL, Order.EARLIEST, now));
        assertEquals(List.of(upcoming, ongoing, past, cancelled), apply(rows, Filter.ALL, Order.LATEST, now));
        assertEquals(List.of(upcoming, ongoing, past, cancelled), rows, "Never mutate input");
    }

    @Test void reclassifiesWhenTimeAdvancesAndReturnsEmptyForNoMatches() {
        assertEquals(List.of(ongoing, upcoming), apply(rows, Filter.PAST, Order.EARLIEST, now.plusSeconds(3600))
                .stream().filter(row -> row != past).toList());
        assertTrue(apply(rows, Filter.ONGOING, Order.EARLIEST, now.plusSeconds(3600)).isEmpty());
        assertTrue(apply(List.of(), Filter.ALL, Order.EARLIEST, now).isEmpty());
    }
}
