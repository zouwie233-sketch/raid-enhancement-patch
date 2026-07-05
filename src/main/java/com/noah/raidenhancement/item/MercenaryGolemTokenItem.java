package com.noah.raidenhancement.item;

import com.noah.raidenhancement.config.BattleSupportConfig;
import com.noah.raidenhancement.raid.MercenaryGolemController;
import com.noah.raidenhancement.raid.SupportTokenCooldowns;
import com.noah.raidenhancement.raid.VictorySettlementController;
import com.noah.raidenhancement.compat.ComponentCompat;
import com.noah.raidenhancement.compat.ItemUseCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Summons temporary player-following mercenary iron golems during village raids. */
public final class MercenaryGolemTokenItem extends Item {
    public MercenaryGolemTokenItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        long now = serverLevel.getGameTime();
        long cooldownRemaining = SupportTokenCooldowns.remainingTicks(player.getUUID(), "mercenary_golem_token", now);
        if (cooldownRemaining > 0L) {
            sendMessage(player, "[村民防卫同盟] 雇佣兵契约正在整备中，约 " + ((cooldownRemaining + 19L) / 20L) + " 秒后可再次签发。");
            return InteractionResultHolder.fail(stack);
        }

        MercenaryGolemController.Result result = MercenaryGolemController.useToken(serverLevel, player);
        if (BattleSupportConfig.SHOW_CHAT_MESSAGES && result.message() != null && !result.message().isBlank()) {
            sendMessage(player, result.message());
        }
        if (!result.success()) {
            return InteractionResultHolder.fail(stack);
        }
        VictorySettlementController.recordPlayerSupportUse(serverLevel, player);
        if (BattleSupportConfig.CONSUME_ITEM_ON_SUCCESS && !ItemUseCompat.isCreativeLike(player)) {
            ItemUseCompat.shrinkItemStack(stack, 1);
        }
        int cooldown = Math.max(0, BattleSupportConfig.MERCENARY_COOLDOWN_TICKS);
        if (cooldown > 0) {
            SupportTokenCooldowns.start(player.getUUID(), "mercenary_golem_token", now, cooldown);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private static void sendMessage(Player player, String rawMessage) {
        if (!BattleSupportConfig.SHOW_CHAT_MESSAGES || rawMessage == null || rawMessage.isBlank()) {
            return;
        }
        Component message = ComponentCompat.literal(rawMessage);
        if (message != null) {
            ItemUseCompat.sendStatusMessage(player, message);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Component line1 = ComponentCompat.translatable("tooltip.raid_enhancement_patch.mercenary_golem_token");
        Component line2 = ComponentCompat.translatable("tooltip.raid_enhancement_patch.common.mercenary_contract");
        if (line1 != null) {
            tooltip.add(line1);
        }
        if (line2 != null) {
            tooltip.add(line2);
        }
    }
}
