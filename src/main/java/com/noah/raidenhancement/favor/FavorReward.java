package com.noah.raidenhancement.favor;

import com.noah.raidenhancement.config.VillageFavorConfig;
import com.noah.raidenhancement.config.VictorySettlementConfig;
import com.noah.raidenhancement.raid.VictorySettlementController;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Handles only favor gift calculation and server-side delivery. */
public final class FavorReward {
    private FavorReward() {
    }

    public static boolean tryGiveGift(ServerLevel level, Player player, VillageFavorRecord record, Entity villager, long gameTime) {
        if (level == null || player == null || record == null || !VillageFavorConfig.ENABLE_GIFT) {
            return false;
        }
        if (!VillageFavorState.canClaimGift(record, gameTime)) {
            return false;
        }

        VillagerProfessionResolver.ProfessionInfo profession = VillagerProfessionResolver.resolve(villager);
        FavorGiftContext context = GiftTierResolver.resolve(record, profession);
        List<ItemStack> stacks = new ArrayList<>(GiftLootResolver.roll(context));
        ItemStack emeraldBonus = bonusEmeraldStack(context);
        if (emeraldBonus != null && !emeraldBonus.isEmpty()) {
            stacks.add(emeraldBonus);
        }
        if (stacks.isEmpty()) {
            return false;
        }
        int given = 0;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            giveOrDrop(level, player, stack.copy());
            given++;
        }
        if (given > 0) {
            VillageFavorState.markGift(level, record, gameTime);
            if (VillageFavorConfig.ENABLE_GIFT_DEBUG_LOG) {
                System.out.println("[Raid Enhancement Patch] Village favor gift: player=" + player.getUUID()
                        + ", village=" + record.key
                        + ", group=" + context.professionGroup()
                        + ", villagerLevel=" + context.villagerLevel()
                        + ", favorLevel=" + record.favorLevel
                        + ", tier=" + context.giftTierName()
                        + ", stacks=" + given + ".");
            }
            return true;
        }
        return false;
    }

    /** Legacy V1 call path retained as safe generic fallback. */
    public static boolean tryGiveGift(ServerLevel level, Player player, VillageFavorRecord record, long gameTime) {
        return tryGiveGift(level, player, record, null, gameTime);
    }

    private static ItemStack bonusEmeraldStack(FavorGiftContext context) {
        if (context == null || context.emeraldBonusCap() <= 0 || VillageFavorConfig.MAX_EMERALD_PER_GIFT <= 0) {
            return ItemStack.EMPTY;
        }
        int cap = Math.max(0, Math.min(VillageFavorConfig.MAX_EMERALD_PER_GIFT, context.emeraldBonusCap()));
        if (cap <= 0) {
            return ItemStack.EMPTY;
        }
        int base = Math.max(0, Math.min(cap, context.giftTier() / 2));
        int randomExtra = cap > base ? ThreadLocalRandom.current().nextInt(0, cap - base + 1) : 0;
        int count = Math.max(0, Math.min(cap, base + randomExtra));
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        return VictorySettlementController.makeItemStack("minecraft:emerald", count);
    }

    private static void giveOrDrop(ServerLevel level, Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }
        boolean added = false;
        if (VictorySettlementConfig.GIVE_REWARDS_TO_INVENTORY) {
            try {
                added = player.addItem(stack);
            } catch (Throwable ignored) {
                added = false;
            }
        }
        if (!added && VictorySettlementConfig.DROP_REWARDS_IF_INVENTORY_FULL) {
            try {
                Method dropMethod = player.getClass().getMethod("drop", ItemStack.class, boolean.class);
                dropMethod.invoke(player, stack, false);
            } catch (Throwable ignored) {
                try {
                    ItemEntity entity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), stack);
                    level.addFreshEntity(entity);
                } catch (Throwable ignoredAgain) {
                    // Favor gift failure must not affect villager interaction or raids.
                }
            }
        }
    }
}
