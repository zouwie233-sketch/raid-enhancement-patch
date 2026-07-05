package com.noah.raidenhancement.item;

import com.noah.raidenhancement.config.BattleSupportConfig;
import com.noah.raidenhancement.raid.BattleSupportController;
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

/** One-shot right-click support token that buffs current village-security golems only. */
public final class BattleSupportTokenItem extends Item {
    private final Kind kind;

    public BattleSupportTokenItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        long now = serverLevel.getGameTime();
        long cooldownRemaining = SupportTokenCooldowns.remainingTicks(player.getUUID(), kind.id(), now);
        if (cooldownRemaining > 0L) {
            sendMessage(player, "[村民防卫同盟] 同类战备令正在整备中，约 " + ((cooldownRemaining + 19L) / 20L) + " 秒后可再次下达。");
            return InteractionResultHolder.fail(stack);
        }

        BattleSupportController.Result result = BattleSupportController.applyToken(serverLevel, player, kind);
        if (BattleSupportConfig.SHOW_CHAT_MESSAGES && result.message() != null && !result.message().isBlank()) {
            sendMessage(player, result.message());
        }
        if (!result.success()) {
            return InteractionResultHolder.fail(stack);
        }
        VictorySettlementController.recordPlayerSupportUse(serverLevel, player);
        if (BattleSupportConfig.CONSUME_ITEM_ON_SUCCESS && kind.consumesOnSuccess() && !ItemUseCompat.isCreativeLike(player)) {
            ItemUseCompat.shrinkItemStack(stack, 1);
        }
        int cooldown = Math.max(0, kind.cooldownTicks());
        if (cooldown > 0) {
            SupportTokenCooldowns.start(player.getUUID(), kind.id(), now, cooldown);
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
        Component line1 = ComponentCompat.translatable("tooltip.raid_enhancement_patch." + kind.id());
        Component line2 = ComponentCompat.translatable(kind.isRaidControlToken()
                ? "tooltip.raid_enhancement_patch.common.raid_control_token"
                : "tooltip.raid_enhancement_patch.common.security_golems_only");
        if (line1 != null) {
            tooltip.add(line1);
        }
        if (line2 != null) {
            tooltip.add(line2);
        }
    }

    public enum Kind {
        BASIC_STRENGTH("basic_strength_token"),
        ADVANCED_STRENGTH("advanced_strength_token"),
        BASIC_SHIELD("basic_shield_token"),
        ADVANCED_SHIELD("advanced_shield_token"),
        BASIC_SWIFTNESS("basic_swiftness_token"),
        ADVANCED_SWIFTNESS("advanced_swiftness_token"),
        HUNTER("hunter_token"),
        BASIC_FIRE("basic_fire_token"),
        ADVANCED_FIRE("advanced_fire_token"),
        BASIC_INSIGHT("basic_insight_token"),
        ADVANCED_INSIGHT("advanced_insight_token"),
        BASIC_RALLY_ENEMY("basic_rally_enemy_token"),
        ADVANCED_RALLY_ENEMY("advanced_rally_enemy_token");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public int cooldownTicks() {
            return switch (this) {
                case BASIC_STRENGTH, ADVANCED_STRENGTH -> BattleSupportConfig.STRENGTH_COOLDOWN_TICKS;
                case BASIC_SHIELD, ADVANCED_SHIELD -> BattleSupportConfig.SHIELD_COOLDOWN_TICKS;
                case BASIC_SWIFTNESS, ADVANCED_SWIFTNESS -> BattleSupportConfig.SWIFTNESS_COOLDOWN_TICKS;
                case HUNTER -> BattleSupportConfig.HUNTER_COOLDOWN_TICKS;
                case BASIC_FIRE, ADVANCED_FIRE -> BattleSupportConfig.FIRE_COOLDOWN_TICKS;
                case BASIC_INSIGHT, ADVANCED_INSIGHT -> BattleSupportConfig.INSIGHT_COOLDOWN_TICKS;
                case BASIC_RALLY_ENEMY -> BattleSupportConfig.RALLY_BASIC_COOLDOWN_TICKS;
                case ADVANCED_RALLY_ENEMY -> BattleSupportConfig.RALLY_ADVANCED_COOLDOWN_TICKS;
            };
        }

        public boolean consumesOnSuccess() {
            return switch (this) {
                case BASIC_RALLY_ENEMY -> BattleSupportConfig.RALLY_BASIC_CONSUMES_ITEM;
                case ADVANCED_RALLY_ENEMY -> BattleSupportConfig.RALLY_ADVANCED_CONSUMES_ITEM;
                default -> true;
            };
        }

        public boolean isRaidControlToken() {
            return this == BASIC_INSIGHT || this == ADVANCED_INSIGHT
                    || this == BASIC_RALLY_ENEMY || this == ADVANCED_RALLY_ENEMY;
        }
    }
}
