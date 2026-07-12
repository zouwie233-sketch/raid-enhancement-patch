package com.noah.raidenhancement.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compatibility bridge for Minecraft mob effects.
 *
 * <p>Runtime descriptors remain reflection-based for optional/staged compatibility, but all
 * class, field, constructor and method discovery is cached, including failed lookups. Normal
 * effect maintenance therefore performs only cached member invocation and never scans methods
 * or constructors on the server tick path.</p>
 */
public final class MobEffectCompat {
    public static final String[] RESISTANCE_NAMES = {"DAMAGE_RESISTANCE", "RESISTANCE"};
    public static final String[] REGENERATION_NAMES = {"REGENERATION"};
    public static final String[] ABSORPTION_NAMES = {"ABSORPTION"};
    public static final String[] FIRE_RESISTANCE_NAMES = {"FIRE_RESISTANCE"};
    public static final String[] GLOWING_NAMES = {"GLOWING"};
    public static final String[] HERO_OF_THE_VILLAGE_NAMES = {"HERO_OF_THE_VILLAGE"};
    public static final String[] HEALTH_BOOST_NAMES = {"HEALTH_BOOST"};

    private static final String MOB_EFFECTS_CLASS = "net.minecraft.world.effect.MobEffects";
    private static final String MOB_EFFECT_INSTANCE_CLASS = "net.minecraft.world.effect.MobEffectInstance";

    private static final Map<String, Optional<Object>> EFFECT_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private MobEffectCompat() {
    }

    public static void addEffect(Object livingEntity, String[] effectNames, int durationTicks, int amplifier) {
        addEffect(livingEntity, effectNames, durationTicks, amplifier, false, false);
    }

    public static void addVisibleEffect(Object livingEntity, String[] effectNames, int durationTicks, int amplifier) {
        addEffect(livingEntity, effectNames, durationTicks, amplifier, true, true);
    }

    public static void addEffect(Object livingEntity, String[] effectNames, int durationTicks, int amplifier,
                                 boolean visible, boolean showIcon) {
        if (livingEntity == null || effectNames == null || effectNames.length == 0) {
            return;
        }
        try {
            Object effect = resolveEffect(effectNames);
            if (effect == null) {
                warnOnce("missing-effect-" + effectNames[0], "Unable to resolve MobEffect " + String.join("/", effectNames));
                return;
            }
            Object instance = createMobEffectInstance(effect, durationTicks, amplifier, visible, showIcon);
            if (instance == null) {
                warnOnce("missing-instance-ctor", "Unable to construct MobEffectInstance reflectively.");
                return;
            }
            Method addEffect = CachedReflection.findMethod(livingEntity.getClass(), "addEffect", instance);
            if (addEffect == null) {
                warnOnce("missing-addEffect", "Unable to find LivingEntity.addEffect method reflectively.");
                return;
            }
            addEffect.invoke(livingEntity, instance);
        } catch (Throwable throwable) {
            warnOnce("addEffect-" + effectNames[0], "Failed to apply mob effect " + effectNames[0] + ": " + throwable);
        }
    }

    public static void ensureEffect(Object livingEntity, String[] effectNames, int durationTicks, int amplifier,
                                    int refreshWhenRemainingAtOrBelow) {
        int threshold = Math.max(0, refreshWhenRemainingAtOrBelow);
        if (remainingDuration(livingEntity, effectNames) > threshold) {
            return;
        }
        addEffect(livingEntity, effectNames, durationTicks, amplifier, false, false);
    }

    public static void removeEffect(Object livingEntity, String[] effectNames) {
        if (livingEntity == null || effectNames == null || effectNames.length == 0) {
            return;
        }
        try {
            Object effect = resolveEffect(effectNames);
            if (effect == null) {
                return;
            }
            Method removeEffect = CachedReflection.findMethod(livingEntity.getClass(), "removeEffect", effect);
            if (removeEffect == null) {
                warnOnce("missing-removeEffect", "Unable to find LivingEntity.removeEffect method reflectively.");
                return;
            }
            removeEffect.invoke(livingEntity, effect);
        } catch (Throwable throwable) {
            warnOnce("removeEffect-" + effectNames[0], "Failed to remove mob effect " + effectNames[0] + ": " + throwable);
        }
    }

    public static boolean hasEffect(Object livingEntity, String[] effectNames) {
        if (livingEntity == null || effectNames == null || effectNames.length == 0) {
            return false;
        }
        try {
            Object effect = resolveEffect(effectNames);
            if (effect == null) {
                return false;
            }
            Method hasEffect = CachedReflection.findMethod(livingEntity.getClass(), "hasEffect", effect);
            if (hasEffect != null) {
                Object result = hasEffect.invoke(livingEntity, effect);
                return result instanceof Boolean bool && bool;
            }
            Method getEffect = CachedReflection.findMethod(livingEntity.getClass(), "getEffect", effect);
            return getEffect != null && getEffect.invoke(livingEntity, effect) != null;
        } catch (Throwable throwable) {
            warnOnce("hasEffect-" + effectNames[0], "Failed to check mob effect " + effectNames[0] + ": " + throwable);
            return false;
        }
    }

    public static int remainingDuration(Object livingEntity, String[] effectNames) {
        if (livingEntity == null || effectNames == null || effectNames.length == 0) {
            return 0;
        }
        try {
            Object effect = resolveEffect(effectNames);
            if (effect == null) {
                return 0;
            }
            Method getEffect = CachedReflection.findMethod(livingEntity.getClass(), "getEffect", effect);
            if (getEffect == null) {
                return 0;
            }
            Object instance = getEffect.invoke(livingEntity, effect);
            if (instance == null) {
                return 0;
            }
            Method getDuration = CachedReflection.findMethod(instance.getClass(), "getDuration");
            if (getDuration == null) {
                return 0;
            }
            Object value = getDuration.invoke(instance);
            return value instanceof Number number ? Math.max(0, number.intValue()) : 0;
        } catch (Throwable throwable) {
            warnOnce("duration-" + effectNames[0], "Failed to read mob effect duration " + effectNames[0] + ": " + throwable);
            return 0;
        }
    }

    public static void addEffectIfLonger(Object livingEntity, String[] effectNames, int durationTicks, int amplifier) {
        if (remainingDuration(livingEntity, effectNames) >= Math.max(1, durationTicks)) {
            return;
        }
        addEffect(livingEntity, effectNames, durationTicks, amplifier, false, false);
    }

    private static Object resolveEffect(String[] names) {
        for (String name : names) {
            Optional<Object> cached = EFFECT_CACHE.computeIfAbsent(name, MobEffectCompat::resolveSingleEffect);
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        return null;
    }

    private static Optional<Object> resolveSingleEffect(String name) {
        try {
            Class<?> mobEffectsClass = CachedReflection.findClass(MOB_EFFECTS_CLASS);
            if (mobEffectsClass == null) {
                return Optional.empty();
            }
            java.lang.reflect.Field field = CachedReflection.findField(mobEffectsClass, name);
            if (field == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(field.get(null));
        } catch (Throwable throwable) {
            warnOnce("resolve-effect-" + name, "Unable to resolve MobEffect " + name + ": " + throwable);
            return Optional.empty();
        }
    }

    private static Object createMobEffectInstance(Object effect, int durationTicks, int amplifier,
                                                   boolean visible, boolean showIcon) {
        try {
            Class<?> instanceClass = CachedReflection.findClass(MOB_EFFECT_INSTANCE_CLASS);
            if (instanceClass == null) {
                return null;
            }
            int duration = Math.max(1, durationTicks);
            int level = Math.max(0, amplifier);

            Object[][] candidates = {
                    {effect, duration, level, false, visible, showIcon},
                    {effect, duration, level, false, visible},
                    {effect, duration, level},
                    {effect, duration}
            };
            for (Object[] arguments : candidates) {
                Constructor<?> constructor = CachedReflection.findConstructor(instanceClass, arguments);
                if (constructor != null) {
                    return constructor.newInstance(arguments);
                }
            }
        } catch (Throwable throwable) {
            warnOnce("create-instance", "Unable to construct MobEffectInstance: " + throwable);
        }
        return null;
    }

    private static void warnOnce(String key, String message) {
        if (WARNED.add(key)) {
            System.out.println("[Raid Enhancement Patch] " + message);
        }
    }
}
