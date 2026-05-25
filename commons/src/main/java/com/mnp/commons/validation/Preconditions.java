package com.mnp.commons.validation;

import java.util.Collection;
import java.util.Objects;

/**
 * Fail-fast preconditions that rely on {@link Objects#nonNull} / {@link Objects#isNull}
 * instead of {@code var != null}.
 */
public final class Preconditions {

    private Preconditions() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Throw if value is null.
     *
     * @param value value to check
     * @param name  parameter name for error message
     * @param <T>   value type
     * @return the same non-null value
     */
    public static <T> T ensureNonNull(final T value, final String name) {
        if (Objects.isNull(value)) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }

    /**
     * Throw if collection is null or empty.
     */
    public static <T extends Collection<?>> T ensureNotEmpty(final T collection, final String name) {
        ensureNonNull(collection, name);
        if (collection.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return collection;
    }

    /**
     * Throw if text is null or blank.
     */
    public static String ensureNotBlank(final String text, final String name) {
        ensureNonNull(text, name);
        if (text.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }
}
