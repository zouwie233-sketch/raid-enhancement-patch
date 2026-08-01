package com.noah.raidenhancement.runtime;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Server-scoped, non-gameplay diagnostic rate limits and warn-once markers. */
public final class RaidDiagnosticsContext implements AutoCloseable {
    private static final int MAX_RATE_LIMIT_ENTRIES = 512;
    private static final int RETAINED_RATE_LIMIT_ENTRIES = 384;

    private final Map<String, Long> lastLogByEventAndKey = new LinkedHashMap<>();
    private final Set<String> warnedKeys = new LinkedHashSet<>();
    private boolean closed;

    public synchronized boolean shouldLog(String event, String key, long gameTime, long intervalTicks) {
        ensureOpen();
        String mapKey = safe(event) + "|" + safe(key);
        long interval = Math.max(20L, intervalTicks);
        Long previous = lastLogByEventAndKey.get(mapKey);
        if (previous != null && gameTime >= 0L && previous >= 0L && gameTime - previous < interval) {
            return false;
        }
        lastLogByEventAndKey.put(mapKey, gameTime);
        trimRateLimits();
        return true;
    }

    public synchronized boolean warnOnce(String warningKey) {
        ensureOpen();
        return warnedKeys.add(safe(warningKey));
    }

    public synchronized int rateLimitEntryCount() {
        return lastLogByEventAndKey.size();
    }

    public synchronized int warningCount() {
        return warnedKeys.size();
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        lastLogByEventAndKey.clear();
        warnedKeys.clear();
    }

    private void trimRateLimits() {
        if (lastLogByEventAndKey.size() <= MAX_RATE_LIMIT_ENTRIES) {
            return;
        }
        int toRemove = Math.max(1, lastLogByEventAndKey.size() - RETAINED_RATE_LIMIT_ENTRIES);
        for (String oldest : List.copyOf(lastLogByEventAndKey.keySet())) {
            lastLogByEventAndKey.remove(oldest);
            if (--toRemove <= 0) {
                break;
            }
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Raid diagnostics context is closed");
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
