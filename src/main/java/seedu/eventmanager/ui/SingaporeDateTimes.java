package seedu.eventmanager.ui;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Objects;

/** Presentation conversions between Singapore local time and stored UTC instants. */
public final class SingaporeDateTimes {
    public static final ZoneId ZONE = ZoneId.of("Asia/Singapore");

    private static final DateTimeFormatter INPUT_TIME = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .toFormatter(Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FORM_TIME =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).withZone(ZONE);
    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("d MMM uuuu, h:mm a 'SGT'", Locale.ENGLISH)
                    .withZone(ZONE);

    private SingaporeDateTimes() {
    }

    public static Instant toInstant(LocalDate date, String time) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(time, "time");
        LocalTime localTime = LocalTime.parse(time.strip(), INPUT_TIME);
        return LocalDateTime.of(date, localTime).atZone(ZONE).toInstant();
    }

    public static LocalDate dateOf(Instant instant) {
        return Objects.requireNonNull(instant, "instant").atZone(ZONE).toLocalDate();
    }

    public static String timeOf(Instant instant) {
        return FORM_TIME.format(Objects.requireNonNull(instant, "instant"));
    }

    public static String display(Instant instant) {
        return DISPLAY.format(Objects.requireNonNull(instant, "instant"));
    }
}
