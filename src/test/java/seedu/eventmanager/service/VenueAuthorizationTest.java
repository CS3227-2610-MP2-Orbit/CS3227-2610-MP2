package seedu.eventmanager.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;

class VenueAuthorizationTest {
    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final VenueRequest REQUEST = new VenueRequest(REQUEST_ID, UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(1),
            10, VenueRequestStatus.SUBMITTED);

    @Test
    void unauthenticatedActorCannotApprove() {
        assertThrowsCode("UNAUTHENTICATED", () -> service().approve(new Actor(null, Role.VENUE_ADMINISTRATOR), REQUEST_ID));
    }

    @Test
    void organizerCannotApprove() {
        assertThrowsCode("FORBIDDEN", () -> service().approve(new Actor(USER_ID, Role.CLUB_ORGANIZER), REQUEST_ID));
    }

    @Test
    void attendeeCannotApprove() {
        assertThrowsCode("FORBIDDEN", () -> service().approve(new Actor(USER_ID, Role.ATTENDEE), REQUEST_ID));
    }

    @Test
    void administratorWithoutResourceScopeCannotApprove() {
        AuthorizationService restricted = new AuthorizationService() {
            @Override
            public void requireVenueRequestAccess(Actor actor, VenueRequest request) {
                throw new ApplicationException("FORBIDDEN", "The administrator has no access to this venue.");
            }
        };
        assertThrowsCode("FORBIDDEN", () -> service(restricted).approve(
                new Actor(USER_ID, Role.VENUE_ADMINISTRATOR), REQUEST_ID));
    }

    private static VenueAdministratorService service() {
        return service(new AuthorizationService() {
            @Override
            public void requireVenueRequestAccess(Actor actor, VenueRequest request) {
                requireRole(actor, Role.VENUE_ADMINISTRATOR);
                if (request == null) {
                    throw new ApplicationException("RESOURCE_NOT_FOUND", "The requested resource was not found.");
                }
            }
        });
    }

    private static VenueAdministratorService service(AuthorizationService authorization) {
        VenueRequestRepository requests = new VenueRequestRepository() {
            @Override public VenueRequest get(UUID requestId) { return REQUEST; }
            @Override public void save(VenueRequest request) { }
        };
        VenueBookingRepository bookings = new VenueBookingRepository() {
            @Override public boolean hasConflict(UUID venueId, OffsetDateTime startsAt, OffsetDateTime endsAt) {
                return false;
            }
            @Override public void createFromApprovedRequest(VenueRequest request, UUID approverId) { }
        };
        return new VenueAdministratorService(requests, bookings, authorization, (id, event, data) -> { },
                (actor, action, type, id, previous, next, reason) -> { }, new TransactionManager() {
                    @Override public <T> T execute(Supplier<T> work) { return work.get(); }
                });
    }

    private static void assertThrowsCode(String code, org.junit.jupiter.api.function.Executable action) {
        ApplicationException exception = assertThrows(ApplicationException.class, action);
        org.junit.jupiter.api.Assertions.assertEquals(code, exception.code());
    }
}
