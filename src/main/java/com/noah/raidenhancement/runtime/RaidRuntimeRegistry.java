package com.noah.raidenhancement.runtime;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

/** The only process-level lookup table for server-scoped Raid Enhancement runtime contexts. */
public final class RaidRuntimeRegistry {
    private static final ServerScopedContextStore<MinecraftServer, RaidRuntimeContext> CONTEXTS =
            new ServerScopedContextStore<>(RaidRuntimeContext::new);

    private RaidRuntimeRegistry() {
    }

    public static RaidRuntimeContext start(MinecraftServer server) {
        return CONTEXTS.getOrCreate(server);
    }

    public static RaidRuntimeContext require(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            throw new IllegalArgumentException("Server level and owning server are required");
        }
        return start(level.getServer());
    }

    public static Optional<RaidRuntimeContext> find(MinecraftServer server) {
        return CONTEXTS.find(server);
    }

    public static Optional<RaidRuntimeContext> find(ServerLevel level) {
        return level == null ? Optional.empty() : find(level.getServer());
    }

    public static void beginStopping(MinecraftServer server) {
        find(server).ifPresent(RaidRuntimeContext::beginStopping);
    }

    public static void checkpoint(MinecraftServer server) {
        find(server).ifPresent(RaidRuntimeContext::checkpoint);
    }

    public static boolean close(MinecraftServer server) {
        return CONTEXTS.closeAndRemove(server);
    }

    public static int activeContextCount() {
        return CONTEXTS.size();
    }
}
