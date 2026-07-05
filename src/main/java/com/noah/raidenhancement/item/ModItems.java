package com.noah.raidenhancement.item;

import com.noah.raidenhancement.RaidEnhancementPatch;
import com.noah.raidenhancement.compat.ComponentCompat;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the item-driven village battle support tokens added in 0.8.9.7. */
public final class ModItems {
    private ModItems() {
    }

    public static final DeferredRegister<Item> ITEMS = createDeferredRegister("ITEM");
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = createDeferredRegister("CREATIVE_MODE_TAB");

    /**
     * Uses reflection for the NeoForge DeferredRegister factory. Several 1.21.1
     * toolchains compile DeferredRegister.create(ResourceKey, String) as
     * create(Object, String), while the 21.1.234 runtime exposes the ResourceKey
     * descriptor. Keeping this call reflective prevents a startup NoSuchMethodError
     * when the mod is rebuilt through a mismatched mapping/dev stub.
     */
    @SuppressWarnings("unchecked")
    private static <T> DeferredRegister<T> createDeferredRegister(String registryFieldName) {
        try {
            Object registryKey = Class.forName("net.minecraft.core.registries.Registries")
                    .getField(registryFieldName)
                    .get(null);
            Method[] methods = DeferredRegister.class.getMethods();
            RuntimeException lastFailure = null;
            for (Method method : methods) {
                if (!"create".equals(method.getName()) || !Modifier.isStatic(method.getModifiers())
                        || method.getParameterCount() != 2 || method.getParameterTypes()[1] != String.class) {
                    continue;
                }
                try {
                    Object created = method.invoke(null, registryKey, RaidEnhancementPatch.MOD_ID);
                    return (DeferredRegister<T>) created;
                } catch (RuntimeException runtimeException) {
                    lastFailure = runtimeException;
                } catch (ReflectiveOperationException reflectiveOperationException) {
                    lastFailure = new RuntimeException(reflectiveOperationException);
                }
            }
            throw new IllegalStateException("No compatible DeferredRegister.create factory for registry field "
                    + registryFieldName, lastFailure);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to resolve registry field " + registryFieldName, exception);
        }
    }

    public static final DeferredHolder<Item, Item> BASIC_STRENGTH_TOKEN = ITEMS.register("basic_strength_token",
            () -> new BattleSupportTokenItem(BattleSupportTokenItem.Kind.BASIC_STRENGTH, new Item.Properties()));
    public static final DeferredHolder<Item, Item> ADVANCED_STRENGTH_TOKEN = ITEMS.register("advanced_strength_token",
            () -> new BattleSupportTokenItem(BattleSupportTokenItem.Kind.ADVANCED_STRENGTH, new Item.Properties()));

    public static final DeferredHolder<Item, Item> BASIC_SHIELD_TOKEN = ITEMS.register("basic_shield_token",
            () -> new BattleSupportTokenItem(BattleSupportTokenItem.Kind.BASIC_SHIELD, new Item.Properties()));
    public static final DeferredHolder<Item, Item> ADVANCED_SHIELD_TOKEN = ITEMS.register("advanced_shield_token",
            () -> new BattleSupportTokenItem(BattleSupportTokenItem.Kind.ADVANCED_SHIELD, new Item.Properties()));

    public static final DeferredHolder<Item, Item> BASIC_SWIFTNESS_TOKEN = ITEMS.register("basic_swiftness_token",
            () -> new BattleSupportTokenItem(BattleSupportTokenItem.Kind.BASIC_SWIFTNESS, new Item.Properties()));
    public static final DeferredHolder<Item, Item> ADVANCED_SWIFTNESS_TOKEN = ITEMS.register("advanced_swiftness_token",
            () -> new BattleSupportTokenItem(BattleSupportTokenItem.Kind.ADVANCED_SWIFTNESS, new Item.Properties()));

    public static final DeferredHolder<Item, Item> HUNTER_TOKEN = ITEMS.register("hunter_token",
            () -> new BattleSupportTokenItem(BattleSupportTokenItem.Kind.HUNTER, new Item.Properties()));

    public static final DeferredHolder<Item, Item> BASIC_FIRE_TOKEN = ITEMS.register("basic_fire_token",
            () -> new BattleSupportTokenItem(BattleSupportTokenItem.Kind.BASIC_FIRE, new Item.Properties()));
    public static final DeferredHolder<Item, Item> ADVANCED_FIRE_TOKEN = ITEMS.register("advanced_fire_token",
            () -> new BattleSupportTokenItem(BattleSupportTokenItem.Kind.ADVANCED_FIRE, new Item.Properties()));


    public static final DeferredHolder<Item, Item> BASIC_INSIGHT_TOKEN = ITEMS.register("basic_insight_token",
            () -> new BattleSupportTokenItem(BattleSupportTokenItem.Kind.BASIC_INSIGHT, new Item.Properties()));
    public static final DeferredHolder<Item, Item> ADVANCED_INSIGHT_TOKEN = ITEMS.register("advanced_insight_token",
            () -> new BattleSupportTokenItem(BattleSupportTokenItem.Kind.ADVANCED_INSIGHT, new Item.Properties()));

    public static final DeferredHolder<Item, Item> BASIC_RALLY_ENEMY_TOKEN = ITEMS.register("basic_rally_enemy_token",
            () -> new BattleSupportTokenItem(BattleSupportTokenItem.Kind.BASIC_RALLY_ENEMY, new Item.Properties()));
    public static final DeferredHolder<Item, Item> ADVANCED_RALLY_ENEMY_TOKEN = ITEMS.register("advanced_rally_enemy_token",
            () -> new BattleSupportTokenItem(BattleSupportTokenItem.Kind.ADVANCED_RALLY_ENEMY, new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> MERCENARY_GOLEM_TOKEN = ITEMS.register("mercenary_golem_token",
            () -> new MercenaryGolemTokenItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> VILLAGE_SUPPORT_TAB = CREATIVE_TABS.register("village_support",
            () -> CreativeModeTab.builder()
                    .title(ComponentCompat.translatable("itemGroup.raid_enhancement_patch.village_support"))
                    .icon(ModItems::makeTabIcon)
                    .displayItems(new VillageSupportDisplayItemsGenerator())
                    .build());

    private static ItemStack makeTabIcon() {
        // Minecraft 1.21.1 / NeoForge 21.1.234 runtime exposes ItemStack(ItemLike),
        // not ItemStack(Item). Keep the explicit ItemLike cast to avoid a tab icon
        // render crash when the creative inventory asks for this icon.
        return new ItemStack((ItemLike) BASIC_STRENGTH_TOKEN.get());
    }

    /**
     * Named implementation instead of a lambda. The 1.21.1 runtime requires the exact
     * CreativeModeTab.ItemDisplayParameters descriptor; compiling a lambda through mixed
     * mappings can erase it to Object and crash when the creative inventory rebuilds.
     */
    private static final class VillageSupportDisplayItemsGenerator implements CreativeModeTab.DisplayItemsGenerator {
        @Override
        public void accept(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
            // NeoForge 21.1.234 exposes CreativeModeTab.Output#accept(ItemLike).
            // Calling accept(Item) can compile through mismatched stubs but crash at runtime.
            add(output, BASIC_STRENGTH_TOKEN);
            add(output, ADVANCED_STRENGTH_TOKEN);
            add(output, BASIC_SHIELD_TOKEN);
            add(output, ADVANCED_SHIELD_TOKEN);
            add(output, BASIC_SWIFTNESS_TOKEN);
            add(output, ADVANCED_SWIFTNESS_TOKEN);
            add(output, HUNTER_TOKEN);
            add(output, BASIC_FIRE_TOKEN);
            add(output, ADVANCED_FIRE_TOKEN);
            add(output, BASIC_INSIGHT_TOKEN);
            add(output, ADVANCED_INSIGHT_TOKEN);
            add(output, BASIC_RALLY_ENEMY_TOKEN);
            add(output, ADVANCED_RALLY_ENEMY_TOKEN);
            add(output, MERCENARY_GOLEM_TOKEN);
        }

        private static void add(CreativeModeTab.Output output, DeferredHolder<Item, Item> holder) {
            output.accept((ItemLike) holder.get());
        }
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
