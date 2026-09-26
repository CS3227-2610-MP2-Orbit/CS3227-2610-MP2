package seedu.eventmanager.attendee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import seedu.eventmanager.common.EntityNotFoundException;
import seedu.eventmanager.event.Event;
import seedu.eventmanager.event.EventAuditRecord;
import seedu.eventmanager.event.EventStatus;
import seedu.eventmanager.event.JdbcEventRepository;
import seedu.eventmanager.storage.DatabaseMigration;
import seedu.eventmanager.storage.JdbcEventCatalogueRepository;

/** Real JDBC tests in a per-test schema; never truncates shared application tables. */
class JdbcEventCatalogueRepositoryIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-09-25T00:00:00Z");
    private PGSimpleDataSource database;
    private String schema;
    private JdbcEventCatalogueRepository repository;
    private EventCatalogueService service;

    @BeforeEach
    void setUp() throws Exception {
        String url = System.getenv("EVENT_MANAGER_TEST_DB_URL");
        assumeTrue(url != null && !url.isBlank(), "EVENT_MANAGER_TEST_DB_URL is not configured");
        database = new PGSimpleDataSource();
        database.setURL(url);
        database.setUser(System.getenv().getOrDefault("EVENT_MANAGER_TEST_DB_USER", "event_manager"));
        database.setPassword(System.getenv("EVENT_MANAGER_TEST_DB_PASSWORD"));
        String candidate = "attendee_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = database.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + candidate);
        }
        schema = candidate;
        database.setCurrentSchema(schema);
        new DatabaseMigration(database).migrate();
        repository = new JdbcEventCatalogueRepository(database);
        service = new EventCatalogueService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
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
    void sqlFiltersDraftCompletedAndStartBoundaryAndOrdersResults() {
        Event first = insert(1, EventStatus.PUBLISHED, NOW.plusSeconds(3600), "First", "tech");
        Event second = insert(2, EventStatus.PUBLISHED, first.startsAt(), "Second", "tech");
        Event draft = insert(3, EventStatus.DRAFT, first.startsAt(), "Private", "tech");
        insert(4, EventStatus.COMPLETED, first.startsAt(), "Done", "tech");
        insert(5, EventStatus.PUBLISHED, NOW, "Started", "tech");
        insert(6, EventStatus.PUBLISHED, NOW.minusSeconds(1), "Past", "tech");

        assertEquals(List.of(first, second), repository.findUpcomingPublished(NOW));
        assertTrue(repository.findPublishedById(draft.id()).isEmpty());
        assertTrue(repository.findPublishedById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void realSearchCombinesTextClubAndSingaporeCalendarBoundaries() {
        Event wanted = insert(1, EventStatus.PUBLISHED, Instant.parse("2026-09-25T16:00:00Z"),
                "AI 100% workshop", "tech");
        insert(2, EventStatus.PUBLISHED, wanted.startsAt().minusSeconds(1), "AI 100% workshop", "tech");
        insert(3, EventStatus.PUBLISHED, wanted.startsAt(), "AI 100% workshop", "other");
        LocalDate day = LocalDate.of(2026, 9, 26);

        assertEquals(List.of(wanted.id()), service.search(new CatalogueQuery("100%", "tech", day, day))
                .stream().map(CatalogueEvent::id).toList());
        assertTrue(service.search(new CatalogueQuery("' OR 1=1 --", "tech", null, null)).isEmpty());
    }

    @Test
    void detailsRecheckStoredVisibilityAndDoNotChangeEventsOrAudit() throws Exception {
        Event event = insert(1, EventStatus.PUBLISHED, NOW.plusSeconds(3600), "Visible", "tech");
        assertEquals(event.title(), service.getEvent(event.id()).title());
        service.search(CatalogueQuery.all());
        try (var connection = database.getConnection(); var statement = connection.createStatement()) {
            try (var count = statement.executeQuery("SELECT COUNT(*) FROM organizer_event_audit_record")) {
                count.next();
                assertEquals(1, count.getInt(1));
            }
            try (var update = connection.prepareStatement("UPDATE organizer_event SET status = 'DRAFT' WHERE id = ?")) {
                update.setObject(1, event.id());
                update.executeUpdate();
            }
        }
        assertThrows(EntityNotFoundException.class, () -> service.getEvent(event.id()));
        assertTrue(service.search(CatalogueQuery.all()).isEmpty());
    }

    private Event insert(int id, EventStatus status, Instant startsAt, String title, String club) {
        Event event = new Event(new UUID(0, id), club, "private-organizer", title, "Fixture description",
                startsAt, startsAt.plusSeconds(3600), 20, status, 0);
        new JdbcEventRepository(database).create(event, new EventAuditRecord(NOW, "fixture",
                EventAuditRecord.Action.CREATE_EVENT, event.id(), 0));
        return event;
    }
}
