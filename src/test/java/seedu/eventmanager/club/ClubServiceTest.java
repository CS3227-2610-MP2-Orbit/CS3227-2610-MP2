package seedu.eventmanager.club;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.AccessDeniedException;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.event.OrganizerIdentity;

class ClubServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-26T05:00:00Z");
    private static final UUID CHESS_ID = UUID.fromString("00000000-0000-0000-0000-0000000c1b01");
    private static final UUID DRAMA_ID = UUID.fromString("00000000-0000-0000-0000-0000000c1b02");
    private static final UUID THIRD_ID = UUID.fromString("00000000-0000-0000-0000-0000000c1b03");
    private static final Actor ORGANIZER =
            new Actor(UUID.fromString("00000000-0000-0000-0000-00000000000a"), Role.CLUB_ORGANIZER);
    private static final Actor OTHER_ORGANIZER =
            new Actor(UUID.fromString("00000000-0000-0000-0000-00000000000b"), Role.CLUB_ORGANIZER);
    private static final Actor ATTENDEE =
            new Actor(UUID.fromString("00000000-0000-0000-0000-00000000000c"), Role.ATTENDEE);

    private InMemoryClubRepository clubs;
    private ClubService service;

    @BeforeEach
    void setUp() {
        clubs = new InMemoryClubRepository();
        Deque<UUID> ids = new ArrayDeque<>(List.of(CHESS_ID, DRAMA_ID, THIRD_ID));
        service = new ClubService(clubs, ids::pop, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createClub_validName_persistsClubOwnedByActorWithAudit() {
        Club created = service.createClub(ORGANIZER, "Chess Club");

        Club expected = new Club(CHESS_ID, "Chess Club", ORGANIZER.userId(), NOW);
        assertEquals(expected, created);
        assertEquals(List.of(expected), clubs.stored);
        assertEquals(
                List.of(new ClubAuditRecord(NOW, ORGANIZER.userId(), ClubAuditRecord.Action.CREATE_CLUB, CHESS_ID)),
                clubs.auditRecords);
    }

    @Test
    void createClub_nameTrimmed() {
        assertEquals("Chess Club", service.createClub(ORGANIZER, "  Chess Club  ").name());
    }

    @Test
    void createClub_blankName_rejectedWithoutChanges() {
        assertThrows(ValidationException.class, () -> service.createClub(ORGANIZER, "   "));
        assertThrows(ValidationException.class, () -> service.createClub(ORGANIZER, null));

        assertNothingStored();
    }

    @Test
    void createClub_nameAtMaximumLength_accepted() {
        String name = "c".repeat(ClubService.MAX_NAME_LENGTH);

        assertEquals(name, service.createClub(ORGANIZER, name).name());
    }

    @Test
    void createClub_nameTooLong_rejectedWithoutChanges() {
        String name = "c".repeat(ClubService.MAX_NAME_LENGTH + 1);

        assertThrows(ValidationException.class, () -> service.createClub(ORGANIZER, name));

        assertNothingStored();
    }

    @Test
    void createClub_duplicateNameIgnoringCase_rejectedEvenForAnotherOrganizer() {
        service.createClub(ORGANIZER, "Chess Club");

        assertThrows(ValidationException.class, () -> service.createClub(OTHER_ORGANIZER, "  chess CLUB "));

        assertEquals(1, clubs.stored.size());
        assertEquals(1, clubs.auditRecords.size());
    }

    @Test
    void createClub_nonOrganizer_rejectedWithoutChanges() {
        assertThrows(AccessDeniedException.class, () -> service.createClub(ATTENDEE, "Chess Club"));

        assertNothingStored();
    }

    @Test
    void myClubs_onlyActorsClubsSortedByNameIgnoringCase() {
        service.createClub(ORGANIZER, "drama Society");
        service.createClub(OTHER_ORGANIZER, "Robotics");
        service.createClub(ORGANIZER, "Chess Club");

        assertEquals(
                List.of("Chess Club", "drama Society"),
                service.myClubs(ORGANIZER).stream().map(Club::name).toList());
    }

    @Test
    void myClubs_nonOrganizer_rejected() {
        assertThrows(AccessDeniedException.class, () -> service.myClubs(ATTENDEE));
    }

    @Test
    void identityFor_usesAccountIdAndOnlyOwnedClubIds() {
        service.createClub(ORGANIZER, "Chess Club");
        service.createClub(OTHER_ORGANIZER, "Drama Society");

        OrganizerIdentity identity = service.identityFor(ORGANIZER);

        assertEquals(ORGANIZER.userId().toString(), identity.userId());
        assertEquals(Set.of(CHESS_ID.toString()), identity.ownedClubIds());
    }

    @Test
    void identityFor_organizerWithoutClubs_hasNoClubs() {
        assertTrue(service.identityFor(ORGANIZER).ownedClubIds().isEmpty());
    }

    @Test
    void identityFor_nonOrganizer_rejected() {
        assertThrows(AccessDeniedException.class, () -> service.identityFor(ATTENDEE));
        assertThrows(AccessDeniedException.class,
                () -> service.identityFor(new Actor(ATTENDEE.userId(), Role.VENUE_ADMINISTRATOR)));
    }

    private void assertNothingStored() {
        assertTrue(clubs.stored.isEmpty());
        assertTrue(clubs.auditRecords.isEmpty());
    }

    private static final class InMemoryClubRepository implements ClubRepository {
        private final List<Club> stored = new ArrayList<>();
        private final List<ClubAuditRecord> auditRecords = new ArrayList<>();

        @Override
        public List<Club> findByOwner(UUID ownerId) {
            return stored.stream().filter(club -> club.ownerId().equals(ownerId)).toList();
        }

        @Override
        public boolean existsByNameIgnoreCase(String name) {
            String key = name.toLowerCase(Locale.ROOT);
            return stored.stream().anyMatch(club -> club.name().toLowerCase(Locale.ROOT).equals(key));
        }

        @Override
        public void create(Club club, ClubAuditRecord auditRecord) {
            if (existsByNameIgnoreCase(club.name())) {
                throw new ValidationException("duplicate");
            }
            stored.add(club);
            auditRecords.add(auditRecord);
        }
    }
}
