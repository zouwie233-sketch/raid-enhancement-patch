package com.noah.raidenhancement.favor;

import com.noah.raidenhancement.config.VillageFavorConfig;

/** Converts favor level + villager career level into a bounded gift tier. */
public final class GiftTierResolver {
    private GiftTierResolver() {
    }

    public static FavorGiftContext resolve(VillageFavorRecord record, VillagerProfessionResolver.ProfessionInfo profession) {
        int favorLevel = record == null ? 1 : Math.max(1, record.favorLevel);
        int villagerLevel = profession == null ? 1 : Math.max(1, Math.min(5, profession.villagerLevel()));
        int tier = 1;
        if (VillageFavorConfig.ENABLE_FAVOR_LEVEL) {
            tier += Math.min(2, Math.max(0, favorLevel - 1));
        }
        if (VillageFavorConfig.ENABLE_VILLAGER_CAREER_LEVEL_SCALING) {
            if (villagerLevel >= 3) tier += 1;
            if (villagerLevel >= 5) tier += 1;
        }
        tier = Math.max(1, Math.min(Math.max(1, VillageFavorConfig.MAX_GIFT_TIER), tier));
        String tierName = switch (tier) {
            case 1 -> "basic";
            case 2, 3 -> "good";
            case 4 -> "great";
            default -> "master";
        };
        if ("master".equals(tierName) && (!VillageFavorConfig.ENABLE_MASTER_VILLAGER_RARE_GIFT || villagerLevel < 5)) {
            tierName = "great";
        }
        int favorEmerald = Math.min(VillageFavorConfig.MAX_EMERALD_BONUS_BY_FAVOR_LEVEL, Math.max(0, favorLevel - 1));
        int villagerEmerald = Math.min(VillageFavorConfig.MAX_EMERALD_BONUS_BY_VILLAGER_LEVEL, Math.max(0, villagerLevel - 1));
        int emeraldCap = Math.max(0, Math.min(VillageFavorConfig.MAX_EMERALD_PER_GIFT, favorEmerald + villagerEmerald));
        String group = VillageFavorConfig.ENABLE_PROFESSION_GIFT && profession != null ? profession.group() : "generic";
        return new FavorGiftContext(group, villagerLevel, tier, tierName, emeraldCap, villagerLevel >= 5);
    }
}
