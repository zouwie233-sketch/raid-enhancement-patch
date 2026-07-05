package com.noah.raidenhancement.compat;

import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;

/**
 * Runtime-safe Component factory bridge for 1.21.1 NeoForge environments.
 *
 * Some staged builds can compile against a synthetic Component.translatable(String)
 * signature, while the real client exposes the varargs form
 * translatable(String, Object...). This bridge never emits direct calls to those
 * static factories; it resolves them at runtime and falls back to literal text.
 */
public final class ComponentCompat {
    private ComponentCompat() {
    }

    public static Component literal(String text) {
        String value = text == null ? "" : text;
        Component component = invokeLiteral(value);
        if (component != null) {
            return component;
        }
        return invokeTranslatable(value);
    }

    public static Component translatable(String key) {
        String value = key == null ? "" : key;
        Component component = invokeTranslatable(value);
        if (component != null) {
            return component;
        }
        return invokeLiteral(value);
    }

    private static Component invokeLiteral(String value) {
        try {
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Method method = componentClass.getMethod("literal", String.class);
            Object result = method.invoke(null, value);
            return (Component) result;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Component invokeTranslatable(String key) {
        try {
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");

            // Preferred 1.21.x runtime shape: translatable(String, Object...)
            try {
                Method method = componentClass.getMethod("translatable", String.class, Object[].class);
                Object result = method.invoke(null, key, new Object[0]);
                return (Component) result;
            } catch (NoSuchMethodException ignored) {
                // Older/synthetic shape: translatable(String)
            }

            Method method = componentClass.getMethod("translatable", String.class);
            Object result = method.invoke(null, key);
            return (Component) result;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
