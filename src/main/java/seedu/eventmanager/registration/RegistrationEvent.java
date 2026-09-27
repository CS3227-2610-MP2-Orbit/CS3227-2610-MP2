package seedu.eventmanager.registration;

import java.time.Instant;
import java.util.UUID;

/** Locked event snapshot used only by registration workflows. */
public record RegistrationEvent(UUID id, String status, int capacity, Instant startsAt, Instant endsAt) { }
