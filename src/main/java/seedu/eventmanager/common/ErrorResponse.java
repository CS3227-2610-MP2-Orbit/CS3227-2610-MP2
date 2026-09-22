package seedu.eventmanager.common;

/** Stable API error shape for controllers and HTTP adapters. */
public record ErrorResponse(String code, String message, int httpStatus, String correlationId) { }
