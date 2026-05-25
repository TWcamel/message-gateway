package com.mnp.commons.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.mnp.commons.validation.Preconditions;

import java.util.function.Supplier;

/**
 * Snowflake-based ID generator built on hutool.
 * <p>Thread-safe singleton. Worker/datacenter IDs are defaulted by hutool
 * (derived from PID + startup time) which is sufficient for single-node validation.</p>
 */
public final class IdUtils {

    private static final Supplier<Snowflake> SNOWFLAKE_SUPPLIER =
            () -> IdUtil.getSnowflake();

    private static final ThreadLocal<Snowflake> SNOWFLAKE =
            ThreadLocal.withInitial(SNOWFLAKE_SUPPLIER);

    private IdUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Generate a snowflake-style numeric string id.
     *
     * @return non-null id string
     */
    public static String nextId() {
        final Long raw = SNOWFLAKE.get().nextId();
        return Preconditions.ensureNonNull(raw, "snowflake id").toString();
    }

    /**
     * Generate a snowflake id and cast to Long.
     *
     * @return snowflake id as long
     */
    public static long nextLongId() {
        return Long.parseLong(nextId());
    }
}
