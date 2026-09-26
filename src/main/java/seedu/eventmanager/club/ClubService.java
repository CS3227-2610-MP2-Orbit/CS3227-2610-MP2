package seedu.eventmanager.club;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import seedu.eventmanager.common.AccessDeniedException;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.event.EventService;
import seedu.eventmanager.event.OrganizerIdentity;

/** Club Organizer workflow: create clubs and resolve which clubs a signed-in organizer owns. */
public final class ClubService {
    public static final int MAX_NAME_LENGTH = 80;

    private final ClubRepository clubs;
    private final EventService.IdGenerator idGenerator;
    private final Clock clock;

    public ClubService(ClubRepository clubs, EventService.IdGenerator idGenerator, Clock clock) {
        this.clubs = Objects.requireNonNull(clubs, "clubs");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Club createClub(Actor actor, String name) {
        requireOrganizer(actor);
        String normalized = normalizeName(name);
        if (clubs.existsByNameIgnoreCase(normalized)) {
            throw duplicateName(normalized);
        }
        Instant now = clock.instant();
        Club club = new Club(idGenerator.nextId(), normalized, actor.userId(), now);
        clubs.create(club, new ClubAuditRecord(now, actor.userId(), ClubAuditRecord.Action.CREATE_CLUB, club.id()));
        return club;
    }

    public List<Club> myClubs(Actor actor) {
        requireOrganizer(actor);
        return clubs.findByOwner(actor.userId()).stream()
                .sorted(Comparator.comparing(Club::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Organizer identity whose owned clubs are exactly the clubs this account created. */
    public OrganizerIdentity identityFor(Actor actor) {
        requireOrganizer(actor);
        Set<String> clubIds = clubs.findByOwner(actor.userId()).stream()
                .map(club -> club.id().toString())
                .collect(Collectors.toSet());
        return new OrganizerIdentity(actor.userId().toString(), clubIds);
    }

    static ValidationException duplicateName(String name) {
        return new ValidationException("A club named \"" + name + "\" already exists");
    }

    private static void requireOrganizer(Actor actor) {
        Objects.requireNonNull(actor, "actor");
        if (actor.role() != Role.CLUB_ORGANIZER) {
            throw new AccessDeniedException("Only Club Organizer accounts can manage clubs");
        }
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.strip();
        if (normalized.isEmpty()) {
            throw new ValidationException("Club name must not be blank");
        }
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Club name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        return normalized;
    }
}
