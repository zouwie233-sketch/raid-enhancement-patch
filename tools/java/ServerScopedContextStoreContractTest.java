package com.noah.raidenhancement.runtime;

/** JDK-only contract coverage for ARCH-1.1 server identity and diagnostic isolation. */
public final class ServerScopedContextStoreContractTest {
    private ServerScopedContextStoreContractTest() {
    }

    public static void main(String[] args) {
        identityScopeIsNotEqualityScope();
        removalClosesExactlyOnce();
        closeAllIsIdempotent();
        diagnosticsAreIsolatedAndBounded();
        System.out.println("ServerScopedContextStoreContractTest: PASS");
    }

    private static void identityScopeIsNotEqualityScope() {
        ServerScopedContextStore<String, TrackedContext> store =
                new ServerScopedContextStore<>(ignored -> new TrackedContext());
        String firstServer = new String("same-value");
        String secondServer = new String("same-value");
        TrackedContext first = store.getOrCreate(firstServer);
        check(first == store.getOrCreate(firstServer), "same identity must return same context");
        check(first != store.getOrCreate(secondServer), "equal values with different identities must be isolated");
        check(store.size() == 2, "two server identities must create two contexts");
        store.close();
    }

    private static void removalClosesExactlyOnce() {
        ServerScopedContextStore<Object, TrackedContext> store =
                new ServerScopedContextStore<>(ignored -> new TrackedContext());
        Object server = new Object();
        TrackedContext context = store.getOrCreate(server);
        check(store.closeAndRemove(server), "first removal must find context");
        check(context.closeCalls == 1, "removal must close context once");
        check(!store.closeAndRemove(server), "second removal must be a no-op");
        check(context.closeCalls == 1, "second removal must not close again");
    }

    private static void closeAllIsIdempotent() {
        ServerScopedContextStore<Object, TrackedContext> store =
                new ServerScopedContextStore<>(ignored -> new TrackedContext());
        TrackedContext first = store.getOrCreate(new Object());
        TrackedContext second = store.getOrCreate(new Object());
        store.close();
        store.close();
        check(first.closeCalls == 1 && second.closeCalls == 1, "close-all must close every context once");
        check(store.size() == 0, "close-all must remove every context");
    }

    private static void diagnosticsAreIsolatedAndBounded() {
        RaidDiagnosticsContext first = new RaidDiagnosticsContext();
        RaidDiagnosticsContext second = new RaidDiagnosticsContext();
        check(first.shouldLog("event", "raid", 100L, 20L), "first event must log");
        check(!first.shouldLog("event", "raid", 110L, 20L), "same context must rate limit");
        check(second.shouldLog("event", "raid", 110L, 20L), "different context must not inherit rate limit");
        check(first.warnOnce("warning"), "first warning must emit");
        check(!first.warnOnce("warning"), "same warning must be suppressed");
        check(second.warnOnce("warning"), "different context must emit warning independently");
        for (int i = 0; i < 700; i++) {
            first.shouldLog("event", "key-" + i, i * 20L, 20L);
        }
        check(first.rateLimitEntryCount() <= 512, "diagnostic map must stay bounded");
        first.close();
        first.close();
        check(first.isClosed(), "diagnostics close must be idempotent");
        check(first.rateLimitEntryCount() == 0 && first.warningCount() == 0,
                "close must release diagnostic state");
        second.close();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class TrackedContext implements AutoCloseable {
        private int closeCalls;

        @Override
        public void close() {
            closeCalls++;
        }
    }
}
