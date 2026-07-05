package com.noah.raidenhancement.raid;

import java.util.Objects;
import java.util.UUID;

/** Runtime record for a villager protected by an active raid session. */
public record ProtectedVillagerRecord(
        UUID entityUuid,
        int firstProtectedAtTick,
        int lastRefreshTick
) {
    public ProtectedVillagerRecord {
        Objects.requireNonNull(entityUuid, "entityUuid");
    }

    public ProtectedVillagerRecord refreshed(int tick) {
        return new ProtectedVillagerRecord(entityUuid, firstProtectedAtTick, tick);
    }
}
