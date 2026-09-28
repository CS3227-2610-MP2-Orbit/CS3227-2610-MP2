package seedu.eventmanager.registration;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import seedu.eventmanager.attendee.*;
import seedu.eventmanager.common.*;

/** Unit parity against the real command with in-memory storage, not database evidence. */
class CheckInPreviewParityTest {
    static final Instant START = Instant.parse("2030-01-01T10:00:00Z");
    static final Instant END = START.plusSeconds(3600);
    record Case(Registration.Status status, String eventStatus, boolean booking, Instant now,
            CheckInPolicy.Result expected, String code) { }

    static Stream<Case> cases() {
        return Stream.of(
                new Case(null, "PUBLISHED", true, START, CheckInPolicy.Result.NOT_REGISTERED, "REGISTRATION_NOT_FOUND"),
                new Case(Registration.Status.CANCELLED, "COMPLETED", false, END, CheckInPolicy.Result.CANCELLED, "REGISTRATION_CANCELLED"),
                new Case(Registration.Status.CHECKED_IN, "COMPLETED", false, END, CheckInPolicy.Result.ALREADY_CHECKED_IN, "ALREADY_CHECKED_IN"),
                new Case(Registration.Status.CONFIRMED, "PUBLISHED", false, START.minusNanos(1), CheckInPolicy.Result.TOO_EARLY, "CHECK_IN_TOO_EARLY"),
                new Case(Registration.Status.CONFIRMED, "PUBLISHED", true, START, CheckInPolicy.Result.AVAILABLE, null),
                new Case(Registration.Status.CONFIRMED, "PUBLISHED", true, END.minusNanos(1), CheckInPolicy.Result.AVAILABLE, null),
                new Case(Registration.Status.CONFIRMED, "PUBLISHED", false, END, CheckInPolicy.Result.CLOSED, "CHECK_IN_CLOSED"),
                new Case(Registration.Status.CONFIRMED, "DRAFT", true, START.minusNanos(1), CheckInPolicy.Result.CLOSED, "CHECK_IN_CLOSED"),
                new Case(Registration.Status.CONFIRMED, "PUBLISHED", false, START, CheckInPolicy.Result.VENUE_UNAVAILABLE, "CHECK_IN_VENUE_UNAVAILABLE"),
                new Case(Registration.Status.CANCELLED, "PUBLISHED", true, START, CheckInPolicy.Result.CANCELLED, "REGISTRATION_CANCELLED"),
                new Case(Registration.Status.CHECKED_IN, "PUBLISHED", true, START, CheckInPolicy.Result.ALREADY_CHECKED_IN, "ALREADY_CHECKED_IN"));
    }

    @ParameterizedTest @MethodSource("cases")
    void previewsAndCommandShareResultAndPrecedence(Case scenario) {
        var fixture = new RegistrationServiceTest();
        fixture.clock = Clock.fixed(scenario.now(), ZoneOffset.UTC);
        fixture.setup();
        fixture.store.event = new RegistrationEvent(fixture.eventId, scenario.eventStatus(), 10, START, END);
        fixture.store.booking = scenario.booking();
        if (scenario.status() != null) {
            fixture.store.records.put(fixture.alice, new Registration(UUID.randomUUID(), fixture.eventId,
                    fixture.alice, scenario.status(), START.minusSeconds(600),
                    scenario.status() == Registration.Status.CANCELLED ? START.minusSeconds(1) : null,
                    scenario.status() == Registration.Status.CHECKED_IN ? START : null, 0));
        }
        var policy = CheckInPolicy.evaluate(fixture.store.event, scenario.status(), scenario.booking(), scenario.now());
        assertEquals(scenario.expected(), policy);
        var details = new AttendeeEventDetailsService((id, attendee) -> Optional.of(
                new AttendeeEventDetailsRepository.Snapshot(
                        new CatalogueEvent(id, "club", "Workshop", "", START, END, 10), scenario.eventStatus(),
                        Optional.empty(), scenario.booking() ? RegistrationEligibilityPolicy.Booking.CONFIRMED_ACTIVE
                                : RegistrationEligibilityPolicy.Booking.UNCONFIRMED, 0,
                        Optional.ofNullable(scenario.status()), scenario.status() == null ? -1 : 0)),
                token -> new Actor(fixture.alice, Role.ATTENDEE), fixture.clock);
        if (scenario.eventStatus().equals("PUBLISHED") && scenario.now().isBefore(END)) {
            var preview = details.getEvent("alice", fixture.eventId);
            assertEquals(policy, preview.checkInAvailability());
            assertEquals(policy == CheckInPolicy.Result.AVAILABLE, preview.canCheckIn());
        } else {
            // Do not broaden the catalogue to expose ended/unpublished events.
            assertThrows(EntityNotFoundException.class, () -> details.getEvent("alice", fixture.eventId));
        }
        var mine = new MyRegistrationsService(fixture.service::myRegistrations,
                ids -> Map.of(fixture.eventId, new RegistrationEventInfoRepository.EventInfo("Workshop", START,
                        "Room", END, "club", "", scenario.eventStatus(), scenario.booking())),
                token -> new Actor(fixture.alice, Role.ATTENDEE), fixture.clock).list("alice");
        if (scenario.status() == null) assertTrue(mine.isEmpty());
        else {
            assertEquals(policy, mine.getFirst().checkInAvailability());
            assertEquals(policy == CheckInPolicy.Result.AVAILABLE, mine.getFirst().canCheckIn());
        }
        if (scenario.code() == null) {
            assertEquals(Registration.Status.CHECKED_IN,
                    fixture.service.checkIn("alice", fixture.eventId, 0).status());
            assertEquals(List.of("REGISTRATION_CHECKED_IN"), fixture.audits);
        } else {
            RegistrationServiceTest.code(scenario.code(), () -> fixture.service.checkIn("alice", fixture.eventId,
                    scenario.status() == null ? -1 : 0));
            assertTrue(fixture.audits.isEmpty());
        }
        assertTrue(fixture.notices.isEmpty());
    }
}
