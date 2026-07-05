package com.noah.raidenhancement.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compatibility bridge for Minecraft mob effects.
 *
 * The staged builds are compiled outside a full NeoForge Gradle workspace. Direct bytecode
 * references to MobEffects fields or MobEffectInstance constructors can therefore bake in
 * wrong descriptors. This bridge resolves those members reflectively at runtime and degrades
 * to no-op instead of crashing the server if a field name or descriptor differs.
 */
public final class MobEffectCompat {
    public static final String[] RESISTANCE_NAMES = {"DAMAGE_RESISTANCE", "RESISTANCE"};
    public static final String[] REGENERATION_NAMES = {"REGENERATION"};
    public static final String[] ABSORPTION_NAMES = {"ABSORPTION"};
    public static final String[] FIRE_RESISTANCE_NAMES = {"FIRE_RESISTANCE"};
    public static final String[] GLOWING_NAMES = {"GLOWING"};
    public static final String[] HERO_OF_THE_VILLAGE_NAMES = {"HERO_OF_THE_VILLAGE"};

    private static final Map<String, Object> EFFECT_CACHE = new ConcurrentHashMap<>();
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
            Method addEffect = findCompatibleMethod(livingEntity.getClass(), "addEffect", instance.getClass());
            if (addEffect == null) {
                warnOnce("missing-addEffect", "Unable to find LivingEntity.addEffect method reflectively.");
                return;
            }
            addEffect.invoke(livingEntity, instance);
        } catch (Throwable throwable) {
            warnOnce("addEffect-" + effectNames[0], "Failed to apply mob effect " + effectNames[0] + ": " + throwable);
        }
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
            Method removeEffect = findCompatibleMethod(livingEntity.getClass(), "removeEffect", effect.getClass());
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
            Method hasEffect = findCompatibleMethod(livingEntity.getClass(), "hasEffect", effect.getClass());
            if (hasEffect != null) {
                Object result = hasEffect.invoke(livingEntity, effect);
                return result instanceof Boolean bool && bool;
            }
            Method getEffect = findCompatibleMethod(livingEntity.getClass(), "getEffect", effect.getClass());
            if (getEffect != null) {
                return getEffect.invoke(livingEntity, effect) != null;
            }
        } catch (Throwable throwable) {
            warnOnce("hasEffect-" + effectNames[0], "Failed to check mob effect " + effectNames[0] + ": " + throwable);
        }
        return false;
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
            Method getEffect = findCompatibleMethod(livingEntity.getClass(), "getEffect", effect.getClass());
            if (getEffect == null) {
                return 0;
            }
            Object instance = getEffect.invoke(livingEntity, effect);
            if (instance == null) {
                return 0;
            }
            Method getDuration = instance.getClass().getMethod("getDuration");
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
            Object cached = EFFECT_CACHE.get(name);
            if (cached != null) {
                return cached;
            }
        }
        try {
            Class<?> mobEffectsClass = Class.forName("net.minecraft.world.effect.MobEffects");
            for (String name : names) {
                try {
                    Object effect = mobEffectsClass.getField(name).get(null);
                    if (effect != null) {
                        EFFECT_CACHE.put(name, effect);
                        return effect;
                    }
                } catch (NoSuchFieldException ignored) {
                    // Try the next candidate name.
                }
            }
        } catch (Throwable throwable) {
            warnOnce("resolve-effects", "Unable to resolve MobEffects class reflectively: " + throwable);
        }
        return null;
    }

    private static Object createMobEffectInstance(Object effect, int durationTicks, int amplifier, boolean visible, boolean showIcon) {
        try {
            Class<?> instanceClass = Class.forName("net.minecraft.world.effect.MobEffectInstance");
            int duration = Math.max(1, durationTicks);
            int level = Math.max(0, amplifier);
            for (Constructor<?> constructor : instanceClass.getConstructors()) {
                Class<?>[] types = constructor.getParameterTypes();
                if (types.length == 6
                        && types[0].isAssignableFrom(effect.getClass())
                        && types[1] == int.class
                        && types[2] == int.class
                        && types[3] == boolean.class
                        && types[4] == boolean.class
                        && types[5] == boolean.class) {
                    return constructor.newInstance(effect, duration, level, false, visible, showIcon);
                }
            }
            for (Constructor<?> constructor : instanceClass.getConstructors()) {
                Class<?>[] types = constructor.getParameterTypes();
                if (types.length == 5
                        && types[0].isAssignableFrom(effect.getClass())
                        && types[1] == int.class
                        && types[2] == int.class
                        && types[3] == boolean.class
                        && types[4] == boolean.class) {
                    return constructor.newInstance(effect, duration, level, false, visible);
                }
            }
            for (Constructor<?> constructor : instanceClass.getConstructors()) {
                Class<?>[] types = constructor.getParameterTypes();
                if (types.length == 3
                        && types[0].isAssignableFrom(effect.getClass())
                        && types[1] == int.class
                        && types[2] == int.class) {
                    return constructor.newInstance(effect, duration, level);
                }
            }
            for (Constructor<?> constructor : instanceClass.getConstructors()) {
                Class<?>[] types = constructor.getParameterTypes();
                if (types.length == 2
                        && types[0].isAssignableFrom(effect.getClass())
                        && types[1] == int.class) {
                    return constructor.newInstance(effect, duration);
                }
            }
        } catch (Throwable throwable) {
            warnOnce("create-instance", "Unable to construct MobEffectInstance: " + throwable);
        }
        return null;
    }

    private static Method findCompatibleMethod(Class<?> startClass, String methodName, Class<?> argumentClass) {
        Class<?> current = startClass;
        while (current != null) {
            for (Method method : current.getMethods()) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (method.getName().equals(methodName)
                        && parameterTypes.length == 1
                        && parameterTypes[0].isAssignableFrom(argumentClass)) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static void warnOnce(String key, String message) {
        if (WARNED.add(key)) {
            System.out.println("[Raid Enhancement Patch] " + message);
        }
    }
}
