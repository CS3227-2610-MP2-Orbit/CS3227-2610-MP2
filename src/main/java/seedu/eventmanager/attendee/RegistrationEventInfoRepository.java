package seedu.eventmanager.attendee;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Internal batch enrichment of events already authorized by RegistrationService.myRegistrations. */
public interface RegistrationEventInfoRepository {
    Map<UUID, EventInfo> findAll(Set<UUID> eventIds);

    record EventInfo(String title, Instant startsAt, String venue, Instant endsAt,
            String clubId, String description, String eventStatus) { }
}
