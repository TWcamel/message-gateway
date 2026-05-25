package com.mnp.commons.util;

import com.mnp.commons.validation.Preconditions;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Time conversion helpers using functional style (no var != null checks).
 */
public final class TimeUtils {

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_INSTANT;

    private TimeUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Current epoch millis. */
    public static long epochMillis() {
        return Instant.now().toEpochMilli();
    }

    /** Format instant as ISO-8601 string. */
    public static String toIsoString(final Instant instant) {
        final Instant safe = Preconditions.ensureNonNull(instant, "instant");
        return ISO_FORMATTER.format(safe);
    }

    /** Parse ISO-8601 string to Instant. */
    public static Instant fromIsoString(final String iso) {
        final String safe = Preconditions.ensureNonNull(iso, "iso string");
        return Instant.parse(safe);
    }

    /** Instant → LocalDateTime at UTC. */
    public static LocalDateTime toUtcDateTime(final Instant instant) {
        final Instant safe = Preconditions.ensureNonNull(instant, "instant");
        return LocalDateTime.ofInstant(safe, ZoneOffset.UTC);
    }

    /** LocalDateTime (UTC assumed) → Instant. */
    public static LocalDateTime toUtcDateTime(final long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
    }

    /** Safely wrap nullable Instant into Optional chain-friendly form. */
    public static Optional<Instant> optionalInstant(final Instant instant) {
        return Optional.ofNullable(instant);
    }
}
