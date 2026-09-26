package seedu.eventmanager.service;

import java.time.OffsetDateTime;
import java.util.List;
import seedu.eventmanager.venue.VenueUtilization;

public interface VenueUtilizationRepository {
    List<VenueUtilization> findForWindow(OffsetDateTime startsAt, OffsetDateTime endsAt);
}
