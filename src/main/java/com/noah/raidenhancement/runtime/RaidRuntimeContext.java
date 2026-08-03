package com.noah.raidenhancement.runtime;

import com.noah.raidenhancement.persistence.RaidLifecycleSnapshotRepository;
import net.minecraft.server.MinecraftServer;

import java.util.Objects;

/**
 * Lifecycle and dependency boundary for one concrete Minecraft server.
 *
 * <p>The context owns server-lifetime infrastructure only. Gameplay decisions remain in
 * their domain controllers; diagnostics and save-scoped lifecycle persistence are composed
 * here so they cannot outlive or cross their concrete server.</p>
 */
public final class RaidRuntimeContext implements AutoCloseable {
    private final int serverIdentity;
    private final RaidDiagnosticsContext diagnostics = new RaidDiagnosticsContext();
    private final RaidLifecycleSnapshotRepository lifecycleSnapshots;
    private boolean stopping;
    private boolean closed;

    RaidRuntimeContext(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        this.serverIdentity = System.identityHashCode(server);
        this.lifecycleSnapshots = new RaidLifecycleSnapshotRepository(server);
    }

    public synchronized int serverIdentity() {
        return serverIdentity;
    }

    public synchronized RaidDiagnosticsContext diagnostics() {
        ensureOpen();
        return diagnostics;
    }

    public synchronized RaidLifecycleSnapshotRepository lifecycleSnapshots() {
        ensureOpen();
        return lifecycleSnapshots;
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

    public synchronized void checkpoint() {
        ensureOpen();
        lifecycleSnapshots.checkpoint();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        stopping = true;
        try {
            lifecycleSnapshots.close();
        } finally {
            diagnostics.close();
            closed = true;
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Raid runtime context is closed");
        }
    }
}
