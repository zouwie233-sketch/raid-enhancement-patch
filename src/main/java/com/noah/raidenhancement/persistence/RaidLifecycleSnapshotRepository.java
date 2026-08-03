package com.noah.raidenhancement.persistence;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;
import java.util.Properties;

/**
 * Server-scoped gateway to the current save's raid lifecycle metadata.
 *
 * <p>The data is always attached to the current server's Overworld. It never reads the
 * legacy version-global config sidecar, so one save cannot claim another save's raid queue.</p>
 */
public final class RaidLifecycleSnapshotRepository implements AutoCloseable {
    public static final String DATA_FILE_ID = "raid_enhancement_patch_raid_session_lifecycle";

    private MinecraftServer server;
    private RaidLifecycleSavedData savedData;
    private boolean closed;

    public RaidLifecycleSnapshotRepository(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public synchronized Properties load() {
        ensureOpen();
        return RaidLifecyclePropertiesCodec.decode(data().payloadCopy());
    }

    public synchronized void replace(Properties properties) {
        ensureOpen();
        data().replacePayload(RaidLifecyclePropertiesCodec.encode(properties));
    }

    /** SavedData is flushed by the normal authoritative server save that follows stopping. */
    public synchronized void checkpoint() {
        ensureOpen();
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
        savedData = null;
        server = null;
    }

    private RaidLifecycleSavedData data() {
        if (savedData != null) {
            return savedData;
        }
        MinecraftServer currentServer = Objects.requireNonNull(server, "server");
        ServerLevel overworld = currentServer.overworld();
        if (overworld == null) {
            throw new IllegalStateException("Cannot attach raid lifecycle SavedData before the Overworld exists");
        }
        savedData = overworld.getDataStorage().computeIfAbsent(
                RaidLifecycleSavedData.FACTORY, DATA_FILE_ID);
        return savedData;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Raid lifecycle snapshot repository is closed");
        }
    }
}
