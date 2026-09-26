package seedu.eventmanager.attendee;

import java.time.LocalDate;

/** Inclusive Singapore-calendar start-date range; blank text/club means no filter. */
public record CatalogueQuery(String text, String clubId, LocalDate from, LocalDate to) {
    public static CatalogueQuery all() {
        return new CatalogueQuery("", "", null, null);
    }
}
