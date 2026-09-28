package seedu.eventmanager.attendee;

import java.util.Objects;
import java.util.Comparator;

/** Public club identity: the ID is for filtering, the name is for display. */
public record CatalogueClub(String id, String name) {
    public static final Comparator<CatalogueClub> BY_NAME =
            Comparator.comparing(CatalogueClub::name, String.CASE_INSENSITIVE_ORDER).thenComparing(CatalogueClub::id);

    public CatalogueClub {
        Objects.requireNonNull(id);
        name = displayName(name);
    }

    public static String displayName(String name) {
        return name == null || name.isBlank() ? "Unknown club" : name;
    }

    @Override public String toString() { return name; }
}
