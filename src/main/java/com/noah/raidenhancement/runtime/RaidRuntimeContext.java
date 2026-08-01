package com.noah.raidenhancement.runtime;

import net.minecraft.server.MinecraftServer;

import java.util.Objects;

/**
 * Lifecycle and dependency boundary for one concrete Minecraft server.
 *
 * <p>The context intentionally owns no raid gameplay state in ARCH-1.1. Its first migrated
 * component is diagnostic-only, proving server isolation before any authoritative encounter
 * state is moved out of the legacy controllers.</p>
 */
public final class RaidRuntimeContext implements AutoCloseable {
    private final int serverIdentity;
    private final RaidDiagnosticsContext diagnostics = new RaidDiagnosticsContext();
    private boolean stopping;
    private boolean closed;

    RaidRuntimeContext(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        this.serverIdentity = System.identityHashCode(server);
    }

    public synchronized int serverIdentity() {
        return serverIdentity;
    }

    public synchronized RaidDiagnosticsContext diagnostics() {
        ensureOpen();
        return diagnostics;
    }

    public synchronized void beginStopping() {
        if (!closed) {
            stopping = true;
        }
    }

    public synchronized boolean isStopping() {
        return stopping;
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    /** Reserved checkpoint boundary; no durable component is migrated in ARCH-1.1. */
    public synchronized void checkpoint() {
        ensureOpen();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        stopping = true;
        diagnostics.close();
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Raid runtime context is closed");
        }
    }
}
