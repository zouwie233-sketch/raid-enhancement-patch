package com.noah.raidenhancement.favor;

import com.noah.raidenhancement.config.VillageFavorConfig;
import com.noah.raidenhancement.raid.JsonRewardTable;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Resolves profession/tier gift pools and safely falls back to generic pools. */
public final class GiftLootResolver {
    private GiftLootResolver() {
    }

    public static List<ItemStack> roll(FavorGiftContext context) {
        if (context == null) {
            return JsonRewardTable.rollVillageFavorGift();
        }
        Path primary = VillageFavorConfig.giftLootFile(context.professionGroup(), context.giftTierName());
        if (Files.exists(primary)) {
            List<ItemStack> stacks = JsonRewardTable.rollFile(primary);
            if (!stacks.isEmpty()) {
                return stacks;
            }
        }
        if (VillageFavorConfig.FALLBACK_TO_GENERIC_GIFT_POOL) {
            Path generic = VillageFavorConfig.giftLootFile("generic", context.giftTierName());
            if (Files.exists(generic)) {
                List<ItemStack> stacks = JsonRewardTable.rollFile(generic);
                if (!stacks.isEmpty()) {
                    return stacks;
                }
            }
        }
        return JsonRewardTable.rollVillageFavorGift();
    }
}
