package seedu.eventmanager.common;

/** Maps internal application failures to safe, stable API responses. */
public final class ErrorResponseMapper {
    private ErrorResponseMapper() { }

    public static ErrorResponse map(Throwable error, String correlationId) {
        if (error instanceof ApplicationException applicationException) {
            String code = applicationException.code();
            return new ErrorResponse(code, publicMessage(code), status(code), correlationId);
        }
        return new ErrorResponse("INTERNAL_ERROR", "The request could not be completed.", 500, correlationId);
    }

    private static int status(String code) {
        return switch (code) {
            case "UNAUTHENTICATED" -> 401;
            case "FORBIDDEN" -> 403;
            case "REQUEST_NOT_FOUND", "RESOURCE_NOT_FOUND" -> 404;
            case "INVALID_REQUEST", "INVALID_TIME_RANGE", "INVALID_ATTENDANCE",
                    "REJECTION_REASON_REQUIRED", "INVALID_STATE" -> 400;
            case "BOOKING_CONFLICT" -> 409;
            case "TRANSACTION_FAILED" -> 503;
            default -> 422;
        };
    }

    private static String publicMessage(String code) {
        return switch (code) {
            case "UNAUTHENTICATED" -> "Authentication is required.";
            case "FORBIDDEN" -> "You are not allowed to perform this action.";
            case "REQUEST_NOT_FOUND", "RESOURCE_NOT_FOUND" -> "The requested resource was not found.";
            case "BOOKING_CONFLICT" -> "The venue is unavailable for the requested time.";
            case "INVALID_TIME_RANGE" -> "The booking time range is invalid.";
            case "INVALID_ATTENDANCE" -> "Expected attendance must be positive.";
            case "REJECTION_REASON_REQUIRED" -> "A rejection reason is required.";
            case "INVALID_STATE" -> "This action is not valid for the current request state.";
            case "TRANSACTION_FAILED" -> "The operation could not be saved. Please try again.";
            default -> "The request could not be completed.";
        };
    }
}
