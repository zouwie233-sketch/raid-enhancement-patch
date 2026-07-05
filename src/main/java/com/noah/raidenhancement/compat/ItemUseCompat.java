package com.noah.raidenhancement.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Runtime-safe bridge for item-use calls that changed descriptors between staged stubs and the real
 * NeoForge 1.21.1 runtime. This avoids direct calls to Player#getAbilities and Player#getCooldowns,
 * whose return types can differ from handwritten compile stubs.
 */
public final class ItemUseCompat {
    private static boolean warnedCreativeCheck;
    private static boolean warnedShrink;
    private static boolean warnedCooldown;
    private static boolean warnedMessage;

    private ItemUseCompat() {
    }

    public static void sendStatusMessage(Object player, Object component) {
        if (player == null || component == null) {
            return;
        }
        try {
            for (Method method : player.getClass().getMethods()) {
                if (!method.getName().equals("displayClientMessage") || method.getParameterCount() != 2) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                if (types[1] != boolean.class && types[1] != Boolean.TYPE) {
                    continue;
                }
                if (!types[0].isAssignableFrom(component.getClass())) {
                    continue;
                }
                method.setAccessible(true);
                method.invoke(player, component, false);
                return;
            }
            for (Method method : player.getClass().getMethods()) {
                if (!method.getName().equals("sendSystemMessage") || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> type = method.getParameterTypes()[0];
                if (!type.isAssignableFrom(component.getClass())) {
                    continue;
                }
                method.setAccessible(true);
                method.invoke(player, component);
                return;
            }
        } catch (Throwable throwable) {
            warnOnce("message", throwable);
        }
    }

    public static boolean isCreativeLike(Object player) {
        if (player == null) {
            return false;
        }
        try {
            Object abilities = invokeNoArg(player, "getAbilities");
            Boolean instabuild = readBooleanField(abilities, "instabuild");
            if (instabuild != null) {
                return instabuild;
            }
            // Runtime fallback if the method name is remapped but returns an Abilities-like object.
            for (Method method : player.getClass().getMethods()) {
                if (method.getParameterCount() != 0) {
                    continue;
                }
                Class<?> returnType = method.getReturnType();
                String name = returnType.getName();
                if (!"net.minecraft.world.entity.player.Abilities".equals(name)
                        && !"net.minecraft.world.entity.player.Player$Abilities".equals(name)
                        && !returnType.getSimpleName().equals("Abilities")) {
                    continue;
                }
                method.setAccessible(true);
                Object candidate = method.invoke(player);
                instabuild = readBooleanField(candidate, "instabuild");
                if (instabuild != null) {
                    return instabuild;
                }
            }
            Boolean creative = invokeBooleanNoArg(player, "isCreative");
            if (creative != null) {
                return creative;
            }
            creative = invokeBooleanNoArg(player, "isCreativeMode");
            if (creative != null) {
                return creative;
            }
        } catch (Throwable throwable) {
            warnOnce("creative", throwable);
        }
        return false;
    }

    public static void shrinkItemStack(Object stack, int amount) {
        if (stack == null || amount <= 0) {
            return;
        }
        try {
            for (Method method : stack.getClass().getMethods()) {
                if (!method.getName().equals("shrink") || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> type = method.getParameterTypes()[0];
                if (type != int.class && type != Integer.TYPE) {
                    continue;
                }
                method.setAccessible(true);
                method.invoke(stack, amount);
                return;
            }
        } catch (Throwable throwable) {
            warnOnce("shrink", throwable);
        }
    }

    public static void addItemCooldown(Object player, Object item, int ticks) {
        if (player == null || item == null || ticks <= 0) {
            return;
        }
        try {
            Object cooldowns = invokeNoArg(player, "getCooldowns");
            if (cooldowns == null) {
                return;
            }
            for (Method method : cooldowns.getClass().getMethods()) {
                if (!method.getName().equals("addCooldown") || method.getParameterCount() != 2) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                if (!types[0].isAssignableFrom(item.getClass())) {
                    continue;
                }
                if (types[1] != int.class && types[1] != Integer.TYPE) {
                    continue;
                }
                method.setAccessible(true);
                method.invoke(cooldowns, item, ticks);
                return;
            }
        } catch (Throwable throwable) {
            warnOnce("cooldown", throwable);
        }
    }

    private static Object invokeNoArg(Object target, String methodName) throws ReflectiveOperationException {
        if (target == null || methodName == null) {
            return null;
        }
        Method method = target.getClass().getMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static Boolean invokeBooleanNoArg(Object target, String methodName) {
        try {
            Object result = invokeNoArg(target, methodName);
            if (result instanceof Boolean bool) {
                return bool;
            }
        } catch (Throwable ignored) {
            // Optional fallback.
        }
        return null;
    }

    private static Boolean readBooleanField(Object target, String fieldName) {
        if (target == null || fieldName == null) {
            return null;
        }
        try {
            Field field = target.getClass().getField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            if (value instanceof Boolean bool) {
                return bool;
            }
        } catch (Throwable ignored) {
            // Try declared fields below.
        }
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            if (value instanceof Boolean bool) {
                return bool;
            }
        } catch (Throwable ignored) {
            // Fail closed.
        }
        return null;
    }

    private static void warnOnce(String type, Throwable throwable) {
        if ("creative".equals(type)) {
            if (warnedCreativeCheck) return;
            warnedCreativeCheck = true;
        } else if ("shrink".equals(type)) {
            if (warnedShrink) return;
            warnedShrink = true;
        } else if ("cooldown".equals(type)) {
            if (warnedCooldown) return;
            warnedCooldown = true;
        } else if ("message".equals(type)) {
            if (warnedMessage) return;
            warnedMessage = true;
        }
        System.out.println("[Raid Enhancement Patch] Item use compatibility bridge suppressed a " + type + " failure once: " + throwable);
    }
}
