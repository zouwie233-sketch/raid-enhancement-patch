package com.noah.raidenhancement.raid;

import java.util.Objects;

/**
 * Lightweight coordinate record for planned raid attack points.
 *
 * Minecraft's BlockPos is deliberately not referenced in this stage, so this
 * class can compile without the full game dependency while preserving the data
 * model needed by future Mixin hooks.
 */
public record AttackPoint(
        int x,
        int y,
        int z,
        AttackPointRole role,
        int plannedVanillaRaiderCount,
        boolean plannedSpecialRaiderPoint
) {
    public AttackPoint {
        Objects.requireNonNull(role, "role");
        if (plannedVanillaRaiderCount < 0) {
            throw new IllegalArgumentException("plannedVanillaRaiderCount cannot be negative");
        }
    }

    public static AttackPoint main(int x, int y, int z, int plannedVanillaRaiderCount) {
        return new AttackPoint(x, y, z, AttackPointRole.MAIN_ATTACK_POINT, plannedVanillaRaiderCount, true);
    }

    public static AttackPoint side(int x, int y, int z, int plannedVanillaRaiderCount) {
        return new AttackPoint(x, y, z, AttackPointRole.SIDE_ATTACK_POINT, plannedVanillaRaiderCount, false);
    }

    public String compact() {
        return role + "@(" + x + "," + y + "," + z + ") planned=" + plannedVanillaRaiderCount;
    }
}
