package seedu.eventmanager.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.storage.DatabaseMigration;
import seedu.eventmanager.storage.JdbcDraftEventDeletion;
import seedu.eventmanager.storage.JdbcEventBookingCheck;
import seedu.eventmanager.storage.JdbcEventCatalogueRepository;
import seedu.eventmanager.storage.JdbcVenueRelease;

/**
 * PostgreSQL integration for publishing: real Organizer and Venue Administrator schemas in a
 * per-test schema that is dropped afterwards, so shared application tables are never cleared.
 */
class EventPublishIntegrationTest {
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final Instant NOW = Instant.parse("2026-09-24T06:00:00Z");
    private static final Instant STARTS = Instant.parse("2026-10-01T10:00:00Z");
    private static final Instant ENDS = Instant.parse("2026-10-01T12:00:00Z");
    private static final OrganizerIdentity ORGANIZER = new OrganizerIdentity("organizer-1", Set.of("club-1"));

    private PGSimpleDataSource database;
    private String schema;
    private JdbcEventRepository events;
    private JdbcEventBookingCheck bookingCheck;
    private JdbcDraftEventDeletion deletion;
    private EventService service;

    @BeforeEach
    void setUp() throws Exception {
        String url = System.getenv("EVENT_MANAGER_TEST_DB_URL");
        assumeTrue(url != null && !url.isBlank(), "EVENT_MANAGER_TEST_DB_URL is not configured");
        database = new PGSimpleDataSource();
        database.setURL(url);
        database.setUser(System.getenv().getOrDefault("EVENT_MANAGER_TEST_DB_USER", "event_manager"));
        database.setPassword(System.getenv("EVENT_MANAGER_TEST_DB_PASSWORD"));
        String candidate = "publish_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = database.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + candidate);
        }
        schema = candidate;
        database.setCurrentSchema(schema + ",public");
        Flyway.configure().dataSource(database).schemas(schema).locations("classpath:db/migration")
                .load().migrate();
        new DatabaseMigration(database).migrate();

        events = new JdbcEventRepository(database);
        bookingCheck = new JdbcEventBookingCheck(database);
        deletion = new JdbcDraftEventDeletion(database);
        service = new EventService(
                events, () -> EVENT_ID, Clock.fixed(NOW, ZoneOffset.UTC), null, bookingCheck, deletion);
        service.createEvent(ORGANIZER, "club-1", new EventDetails("Campus Night", "Demo", STARTS, ENDS, 80));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (schema != null) {
            try (var connection = database.getConnection(); var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    @Test
    void publish_withMatchingConfirmedBooking_persistsWithAuditAndBecomesVisibleToAttendees() throws Exception {
        booking(EVENT_ID, "ACTIVE", "CONFIRMED", STARTS, ENDS);
        var catalogue = new JdbcEventCatalogueRepository(database);
        assertTrue(catalogue.findPublishedNotEnded(NOW).isEmpty());

        Event published = service.publishEvent(ORGANIZER, EVENT_ID, 0);

        assertEquals(published, events.findById(EVENT_ID).orElseThrow());
        assertEquals(EventStatus.PUBLISHED, published.status());
        assertEquals(1, published.version());
        assertEquals(List.of("CREATE_EVENT", "PUBLISH_EVENT"), auditActions());
        assertEquals(List.of(EVENT_ID),
                catalogue.findPublishedNotEnded(NOW).stream().map(entry -> entry.event().id()).toList());
    }

    @Test
    void bookingCheck_rejectsCancelledMismatchedAndInactiveVenueBookings() throws Exception {
        booking(EVENT_ID, "ACTIVE", "CANCELLED", STARTS, ENDS);
        assertFalse(bookingCheck.hasConfirmedActiveBooking(EVENT_ID, STARTS, ENDS));
        assertThrows(ValidationException.class, () -> service.publishEvent(ORGANIZER, EVENT_ID, 0));
        assertEquals(EventStatus.DRAFT, events.findById(EVENT_ID).orElseThrow().status());
        assertEquals(List.of("CREATE_EVENT"), auditActions());

        UUID mismatched = UUID.randomUUID();
        booking(mismatched, "ACTIVE", "CONFIRMED", STARTS, ENDS.plusSeconds(1800));
        assertFalse(bookingCheck.hasConfirmedActiveBooking(mismatched, STARTS, ENDS));

        UUID inactiveVenue = UUID.randomUUID();
        booking(inactiveVenue, "INACTIVE", "CONFIRMED", STARTS, ENDS);
        assertFalse(bookingCheck.hasConfirmedActiveBooking(inactiveVenue, STARTS, ENDS));

        booking(EVENT_ID, "ACTIVE", "CONFIRMED", STARTS, ENDS);
        assertTrue(bookingCheck.hasConfirmedActiveBooking(EVENT_ID, STARTS, ENDS));
        assertFalse(bookingCheck.hasConfirmedActiveBooking(UUID.randomUUID(), STARTS, ENDS));
    }

    @Test
    void findActiveBooking_returnsCurrentBookingWindowAndVenueStatus() throws Exception {
        assertTrue(bookingCheck.findActiveBooking(EVENT_ID).isEmpty());
        booking(EVENT_ID, "ACTIVE", "CANCELLED", STARTS, ENDS);
        assertTrue(bookingCheck.findActiveBooking(EVENT_ID).isEmpty());

        booking(EVENT_ID, "INACTIVE", "AT_RISK", STARTS.minusSeconds(86_400), ENDS);

        assertEquals(new EventBookingCheck.ActiveBooking(STARTS.minusSeconds(86_400), ENDS, false),
                bookingCheck.findActiveBooking(EVENT_ID).orElseThrow());
    }

    @Test
    void editEvent_afterApproval_timeChangeRejectedButRestoringBookedTimesPublishes() throws Exception {
        Instant bookedStart = STARTS.minusSeconds(86_400);
        booking(EVENT_ID, "ACTIVE", "CONFIRMED", bookedStart, ENDS);

        assertThrows(ValidationException.class, () -> service.editEvent(ORGANIZER, EVENT_ID, 0,
                new EventDetails("Campus Night", "Demo", STARTS.plusSeconds(3600), ENDS, 80)));
        assertThrows(ValidationException.class, () -> service.publishEvent(ORGANIZER, EVENT_ID, 0));

        Event restored = service.editEvent(ORGANIZER, EVENT_ID, 0,
                new EventDetails("Campus Night", "Demo", bookedStart, ENDS, 80)).event();
        Event published = service.publishEvent(ORGANIZER, EVENT_ID, restored.version());

        assertEquals(EventStatus.PUBLISHED, events.findById(EVENT_ID).orElseThrow().status());
        assertEquals(bookedStart, published.startsAt());
        assertEquals(List.of("CREATE_EVENT", "EDIT_EVENT", "PUBLISH_EVENT"), auditActions());
    }

    @Test
    void releaseApprovedBooking_draft_cancelsBookingWithdrawsRequestAndUnlocksTimes() throws Exception {
        UUID organizer = OrganizerIds.toUuid("organizer-1");
        booking(EVENT_ID, "ACTIVE", "CONFIRMED", STARTS, ENDS);
        JdbcVenueRelease release = new JdbcVenueRelease(database);

        assertTrue(release.releaseApprovedBooking(EVENT_ID, organizer));

        assertEquals(List.of("CANCELLED|" + organizer + "|true"), queryStrings(
                "SELECT status || '|' || cancelled_by || '|' || (cancelled_at IS NOT NULL AND cancellation_reason <> '')"
                        + " FROM venue_bookings WHERE event_id='" + EVENT_ID + "'"));
        assertEquals(List.of("WITHDRAWN"), queryStrings(
                "SELECT status FROM venue_requests WHERE event_id='" + EVENT_ID + "'"));
        assertEquals(List.of("VENUE_BOOKING_RELEASED|CLUB_ORGANIZER|CONFIRMED|CANCELLED"), queryStrings(
                "SELECT action || '|' || actor_role || '|' || previous_state || '|' || new_state FROM audit_logs"));
        assertTrue(bookingCheck.findActiveBooking(EVENT_ID).isEmpty());

        Event moved = service.editEvent(ORGANIZER, EVENT_ID, 0, new EventDetails(
                "Campus Night", "Demo", STARTS.plusSeconds(86_400), ENDS.plusSeconds(86_400), 80)).event();
        booking(EVENT_ID, "ACTIVE", "CONFIRMED", moved.startsAt(), moved.endsAt());
        assertEquals(EventStatus.PUBLISHED, service.publishEvent(ORGANIZER, EVENT_ID, moved.version()).status());
    }

    @Test
    void releaseApprovedBooking_publishedOrUnbooked_changesNothing() throws Exception {
        JdbcVenueRelease release = new JdbcVenueRelease(database);
        UUID organizer = OrganizerIds.toUuid("organizer-1");
        assertFalse(release.releaseApprovedBooking(EVENT_ID, organizer));

        booking(EVENT_ID, "ACTIVE", "CONFIRMED", STARTS, ENDS);
        service.publishEvent(ORGANIZER, EVENT_ID, 0);

        assertFalse(release.releaseApprovedBooking(EVENT_ID, organizer));
        assertEquals(List.of("CONFIRMED"), queryStrings(
                "SELECT status FROM venue_bookings WHERE event_id='" + EVENT_ID + "'"));
        assertEquals(List.of("APPROVED"), queryStrings(
                "SELECT status FROM venue_requests WHERE event_id='" + EVENT_ID + "'"));
        assertTrue(queryStrings("SELECT action FROM audit_logs").isEmpty());
    }

    @Test
    void publishUpdate_auditFailure_rollsBackStatus() throws Exception {
        booking(EVENT_ID, "ACTIVE", "CONFIRMED", STARTS, ENDS);
        Event draft = events.findById(EVENT_ID).orElseThrow();
        Event published = new Event(draft.id(), draft.clubId(), draft.organizerId(), draft.title(),
                draft.description(), draft.startsAt(), draft.endsAt(), draft.capacity(), EventStatus.PUBLISHED, 1);
        EventAuditRecord invalidAudit =
                new EventAuditRecord(NOW, null, EventAuditRecord.Action.PUBLISH_EVENT, EVENT_ID, 1);

        assertThrows(EventPersistenceException.class, () -> events.update(published, 0, invalidAudit));

        assertEquals(draft, events.findById(EVENT_ID).orElseThrow());
        assertEquals(List.of("CREATE_EVENT"), auditActions());
    }

    @Test
    void publish_bookingReleasedAfterCheck_refusesAndLeavesDraft() throws Exception {
        booking(EVENT_ID, "ACTIVE", "CONFIRMED", STARTS, ENDS);
        JdbcVenueRelease release = new JdbcVenueRelease(database);
        UUID organizer = OrganizerIds.toUuid("organizer-1");
        EventBookingCheck checkThenRelease = new EventBookingCheck() {
            @Override
            public boolean hasConfirmedActiveBooking(UUID eventId, Instant startsAt, Instant endsAt) {
                boolean held = bookingCheck.hasConfirmedActiveBooking(eventId, startsAt, endsAt);
                if (held) {
                    assertTrue(release.releaseApprovedBooking(eventId, organizer));
                }
                return held;
            }

            @Override
            public Optional<EventBookingCheck.ActiveBooking> findActiveBooking(UUID eventId) {
                return bookingCheck.findActiveBooking(eventId);
            }
        };
        EventService racing = new EventService(
                events, () -> EVENT_ID, Clock.fixed(NOW, ZoneOffset.UTC), null, checkThenRelease, deletion);

        ValidationException error = assertThrows(ValidationException.class,
                () -> racing.publishEvent(ORGANIZER, EVENT_ID, 0));

        assertTrue(error.getMessage().contains("confirmed venue booking"));
        assertEquals(EventStatus.DRAFT, events.findById(EVENT_ID).orElseThrow().status());
        assertTrue(bookingCheck.findActiveBooking(EVENT_ID).isEmpty());
        assertEquals(List.of("CREATE_EVENT"), auditActions());
        assertTrue(new JdbcEventCatalogueRepository(database).findPublishedNotEnded(NOW).isEmpty());
    }

    @RepeatedTest(10)
    void publishAndRelease_overlapping_neverLeavesPublishedEventWithoutBooking() throws Exception {
        booking(EVENT_ID, "ACTIVE", "CONFIRMED", STARTS, ENDS);
        JdbcVenueRelease release = new JdbcVenueRelease(database);
        UUID organizer = OrganizerIds.toUuid("organizer-1");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(2);
        CountDownLatch done = new CountDownLatch(2);
        AtomicReference<Exception> publishFailure = new AtomicReference<>();
        AtomicBoolean released = new AtomicBoolean(false);

        pool.submit(() -> {
            start.countDown();
            try {
                start.await();
                service.publishEvent(ORGANIZER, EVENT_ID, 0);
            } catch (Exception exception) {
                publishFailure.set(exception);
            } finally {
                done.countDown();
            }
        });
        pool.submit(() -> {
            start.countDown();
            try {
                start.await();
                released.set(release.releaseApprovedBooking(EVENT_ID, organizer));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        });

        assertTrue(done.await(15, TimeUnit.SECONDS), "publish and release did not finish");
        pool.shutdownNow();

        Event stored = events.findById(EVENT_ID).orElseThrow();
        boolean published = stored.status() == EventStatus.PUBLISHED;
        boolean hasBooking = bookingCheck.findActiveBooking(EVENT_ID).isPresent();
        assertFalse(published && !hasBooking, "a published event must still have its confirmed booking");
        assertFalse(published && released.get(), "release must not succeed after the event is published");
        if (published) {
            assertNull(publishFailure.get());
            assertEquals(List.of("CREATE_EVENT", "PUBLISH_EVENT"), auditActions());
            assertEquals(List.of(EVENT_ID), new JdbcEventCatalogueRepository(database)
                    .findPublishedNotEnded(NOW).stream().map(entry -> entry.event().id()).toList());
        } else {
            assertEquals(EventStatus.DRAFT, stored.status());
            assertTrue(publishFailure.get() instanceof ValidationException);
            assertTrue(released.get());
            assertFalse(hasBooking);
            assertEquals(List.of("CREATE_EVENT"), auditActions());
            assertTrue(new JdbcEventCatalogueRepository(database).findPublishedNotEnded(NOW).isEmpty());
        }
    }

    @Test
    void publish_staleVersionAfterConcurrentPublish_rejected() throws Exception {
        booking(EVENT_ID, "ACTIVE", "CONFIRMED", STARTS, ENDS);
        service.publishEvent(ORGANIZER, EVENT_ID, 0);

        assertThrows(EventVersionConflictException.class, () -> service.publishEvent(ORGANIZER, EVENT_ID, 0));
        assertEquals(List.of("CREATE_EVENT", "PUBLISH_EVENT"), auditActions());
    }

    @Test
    void delete_plainDraft_softDeletesRowWithAuditAndHidesItFromOrganizer() throws Exception {
        Event deleted = service.deleteEvent(ORGANIZER, EVENT_ID, 0);

        assertEquals(deleted, events.findById(EVENT_ID).orElseThrow());
        assertEquals(EventStatus.DELETED, deleted.status());
        assertEquals(List.of("CREATE_EVENT", "DELETE_EVENT"), auditActions());
        assertTrue(service.listEvents(ORGANIZER).isEmpty());
        assertThrows(seedu.eventmanager.common.EntityNotFoundException.class,
                () -> service.getEvent(ORGANIZER, EVENT_ID));
        assertTrue(queryStrings("SELECT action FROM audit_logs").isEmpty());
        assertTrue(new JdbcEventCatalogueRepository(database).findPublishedNotEnded(NOW).isEmpty());
    }

    @Test
    void delete_draftWithApprovedBooking_releasesBookingAndWithdrawsRequestTogether() throws Exception {
        UUID organizer = OrganizerIds.toUuid("organizer-1");
        booking(EVENT_ID, "ACTIVE", "CONFIRMED", STARTS, ENDS);

        service.deleteEvent(ORGANIZER, EVENT_ID, 0);

        assertEquals(List.of("CANCELLED|" + organizer + "|Released because the organizer deleted the event"), queryStrings(
                "SELECT status || '|' || cancelled_by || '|' || cancellation_reason FROM venue_bookings"));
        assertEquals(List.of("WITHDRAWN"), queryStrings("SELECT status FROM venue_requests"));
        assertEquals(List.of("VENUE_BOOKING_RELEASED|EVENT_DELETED"),
                queryStrings("SELECT action || '|' || reason_code FROM audit_logs"));
        assertTrue(bookingCheck.findActiveBooking(EVENT_ID).isEmpty());
        assertEquals(List.of("CREATE_EVENT", "DELETE_EVENT"), auditActions());
    }

    @Test
    void delete_draftWithSubmittedRequest_withdrawsItWithAudit() throws Exception {
        UUID request = submittedRequest(EVENT_ID);

        service.deleteEvent(ORGANIZER, EVENT_ID, 0);

        assertEquals(List.of("WITHDRAWN"), queryStrings("SELECT status FROM venue_requests"));
        assertEquals(List.of("VENUE_REQUEST_WITHDRAWN|VENUE_REQUEST|" + request + "|SUBMITTED|WITHDRAWN"),
                queryStrings("SELECT action || '|' || entity_type || '|' || entity_id || '|' || previous_state"
                        + " || '|' || new_state FROM audit_logs"));
    }

    @Test
    void delete_publishedEvent_refusedAndBookingKept() throws Exception {
        booking(EVENT_ID, "ACTIVE", "CONFIRMED", STARTS, ENDS);
        Event published = service.publishEvent(ORGANIZER, EVENT_ID, 0);

        assertThrows(ValidationException.class, () -> service.deleteEvent(ORGANIZER, EVENT_ID, 1));
        assertThrows(EventVersionConflictException.class, () -> deletion.deleteDraft(
                deletedCopy(published, 2), 1, deleteAudit(2), OrganizerIds.toUuid("organizer-1")));

        assertEquals(published, events.findById(EVENT_ID).orElseThrow());
        assertEquals(List.of("CONFIRMED"), queryStrings("SELECT status FROM venue_bookings"));
        assertEquals(List.of("APPROVED"), queryStrings("SELECT status FROM venue_requests"));
        assertTrue(queryStrings("SELECT action FROM audit_logs").isEmpty());
        assertEquals(List.of("CREATE_EVENT", "PUBLISH_EVENT"), auditActions());
    }

    @Test
    void delete_auditFailureOrStaleVersion_rollsBackEventAndVenueChanges() throws Exception {
        booking(EVENT_ID, "ACTIVE", "CONFIRMED", STARTS, ENDS);
        Event draft = events.findById(EVENT_ID).orElseThrow();
        UUID organizer = OrganizerIds.toUuid("organizer-1");
        EventAuditRecord invalidAudit = new EventAuditRecord(NOW, null, EventAuditRecord.Action.DELETE_EVENT,
                EVENT_ID, 1);

        assertThrows(EventPersistenceException.class,
                () -> deletion.deleteDraft(deletedCopy(draft, 1), 0, invalidAudit, organizer));
        assertThrows(EventVersionConflictException.class,
                () -> deletion.deleteDraft(deletedCopy(draft, 6), 5, deleteAudit(6), organizer));

        assertEquals(draft, events.findById(EVENT_ID).orElseThrow());
        assertEquals(List.of("CONFIRMED"), queryStrings("SELECT status FROM venue_bookings"));
        assertEquals(List.of("APPROVED"), queryStrings("SELECT status FROM venue_requests"));
        assertTrue(queryStrings("SELECT action FROM audit_logs").isEmpty());
        assertEquals(List.of("CREATE_EVENT"), auditActions());
    }

    private static Event deletedCopy(Event event, long version) {
        return new Event(event.id(), event.clubId(), event.organizerId(), event.title(), event.description(),
                event.startsAt(), event.endsAt(), event.capacity(), EventStatus.DELETED, version);
    }

    private static EventAuditRecord deleteAudit(long version) {
        return new EventAuditRecord(NOW, "organizer-1", EventAuditRecord.Action.DELETE_EVENT, EVENT_ID, version);
    }

    private UUID submittedRequest(UUID eventId) throws Exception {
        UUID venue = UUID.randomUUID();
        UUID request = UUID.randomUUID();
        Timestamp now = Timestamp.from(NOW);
        try (var connection = database.getConnection()) {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO venues (venue_id, name, location, capacity, status, created_at, updated_at)
                    VALUES (?, ?, 'Synthetic', 100, 'ACTIVE', ?, ?)""")) {
                statement.setObject(1, venue);
                statement.setString(2, "Venue " + venue);
                statement.setTimestamp(3, now);
                statement.setTimestamp(4, now);
                statement.executeUpdate();
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO venue_requests (request_id, event_id, venue_id, organizer_id,
                        requested_starts_at, requested_ends_at, expected_attendance, status,
                        submitted_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, 80, 'SUBMITTED', ?, ?, ?)""")) {
                statement.setObject(1, request);
                statement.setObject(2, eventId);
                statement.setObject(3, venue);
                statement.setObject(4, OrganizerIds.toUuid("organizer-1"));
                statement.setTimestamp(5, Timestamp.from(STARTS));
                statement.setTimestamp(6, Timestamp.from(ENDS));
                statement.setTimestamp(7, now);
                statement.setTimestamp(8, now);
                statement.setTimestamp(9, now);
                statement.executeUpdate();
            }
        }
        return request;
    }

    private void booking(UUID eventId, String venueStatus, String bookingStatus, Instant startsAt, Instant endsAt)
            throws Exception {
        UUID venue = UUID.randomUUID();
        UUID request = UUID.randomUUID();
        Timestamp now = Timestamp.from(NOW);
        try (var connection = database.getConnection()) {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO venues (venue_id, name, location, capacity, status, created_at, updated_at)
                    VALUES (?, ?, 'Synthetic', 100, ?, ?, ?)""")) {
                statement.setObject(1, venue);
                statement.setString(2, "Venue " + venue);
                statement.setString(3, venueStatus);
                statement.setTimestamp(4, now);
                statement.setTimestamp(5, now);
                statement.executeUpdate();
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO venue_requests (request_id, event_id, venue_id, organizer_id,
                        requested_starts_at, requested_ends_at, expected_attendance, status,
                        submitted_at, decided_at, decided_by, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, 80, 'APPROVED', ?, ?, ?, ?, ?)""")) {
                statement.setObject(1, request);
                statement.setObject(2, eventId);
                statement.setObject(3, venue);
                statement.setObject(4, OrganizerIds.toUuid("organizer-1"));
                statement.setTimestamp(5, Timestamp.from(startsAt));
                statement.setTimestamp(6, Timestamp.from(endsAt));
                statement.setTimestamp(7, now);
                statement.setTimestamp(8, now);
                statement.setObject(9, UUID.randomUUID());
                statement.setTimestamp(10, now);
                statement.setTimestamp(11, now);
                statement.executeUpdate();
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO venue_bookings (booking_id, request_id, event_id, venue_id, status,
                        starts_at, ends_at, confirmed_at, created_at, updated_at,
                        cancelled_at, cancelled_by, cancellation_reason)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""")) {
                boolean cancelled = "CANCELLED".equals(bookingStatus);
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, request);
                statement.setObject(3, eventId);
                statement.setObject(4, venue);
                statement.setString(5, bookingStatus);
                statement.setTimestamp(6, Timestamp.from(startsAt));
                statement.setTimestamp(7, Timestamp.from(endsAt));
                statement.setTimestamp(8, now);
                statement.setTimestamp(9, now);
                statement.setTimestamp(10, now);
                statement.setTimestamp(11, cancelled ? now : null);
                statement.setObject(12, cancelled ? UUID.randomUUID() : null);
                statement.setString(13, cancelled ? "Synthetic cancellation" : null);
                statement.executeUpdate();
            }
        }
    }

    private List<String> queryStrings(String sql) throws Exception {
        try (var connection = database.getConnection();
                var statement = connection.createStatement();
                var rows = statement.executeQuery(sql)) {
            List<String> values = new ArrayList<>();
            while (rows.next()) {
                values.add(rows.getString(1));
            }
            return values;
        }
    }

    private List<String> auditActions() throws Exception {
        try (var connection = database.getConnection();
                var statement = connection.createStatement();
                var rows = statement.executeQuery(
                        "SELECT action FROM organizer_event_audit_record ORDER BY resulting_version, occurred_at")) {
            List<String> actions = new ArrayList<>();
            while (rows.next()) {
                actions.add(rows.getString(1));
            }
            return actions;
        }
    }
}
