package com.noah.raidenhancement.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small shared cache for compatibility-only reflection.
 *
 * <p>Member discovery is performed at most once for a concrete owner/name/argument signature.
 * Successful and failed lookups are both cached, and accessibility is prepared during discovery
 * rather than on every invocation. This class deliberately contains no Minecraft references so
 * optional compatibility code can reuse it without creating hard class-loading dependencies.</p>
 */
public final class CachedReflection {
    private static final Map<String, Optional<Class<?>>> CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<MethodKey, Optional<Method>> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<ConstructorKey, Optional<Constructor<?>>> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();
    private static final Map<FieldKey, Optional<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    private CachedReflection() {
    }

    public static Class<?> findClass(String className) {
        if (className == null || className.isBlank()) {
            return null;
        }
        return CLASS_CACHE.computeIfAbsent(className, CachedReflection::resolveClass).orElse(null);
    }

    public static Method findMethod(Class<?> owner, String methodName, Object... arguments) {
        if (owner == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        MethodKey key = new MethodKey(owner, methodName, argumentTypes(arguments));
        return METHOD_CACHE.computeIfAbsent(key, CachedReflection::resolveMethod).orElse(null);
    }

    public static Constructor<?> findConstructor(Class<?> owner, Object... arguments) {
        if (owner == null) {
            return null;
        }
        ConstructorKey key = new ConstructorKey(owner, argumentTypes(arguments));
        return CONSTRUCTOR_CACHE.computeIfAbsent(key, CachedReflection::resolveConstructor).orElse(null);
    }

    public static Field findField(Class<?> owner, String fieldName) {
        if (owner == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        FieldKey key = new FieldKey(owner, fieldName);
        return FIELD_CACHE.computeIfAbsent(key, CachedReflection::resolveField).orElse(null);
    }

    public static Object invoke(Object target, String methodName, Object... arguments) throws ReflectiveOperationException {
        if (target == null) {
            throw new NullPointerException("target");
        }
        Method method = findMethod(target.getClass(), methodName, arguments);
        if (method == null) {
            throw new NoSuchMethodException(target.getClass().getName() + "." + methodName);
        }
        return method.invoke(target, arguments);
    }

    public static Object construct(Class<?> owner, Object... arguments) throws ReflectiveOperationException {
        Constructor<?> constructor = findConstructor(owner, arguments);
        if (constructor == null) {
            throw new NoSuchMethodException(owner == null ? "null constructor" : owner.getName() + " constructor");
        }
        return constructor.newInstance(arguments);
    }

    public static Object readField(Object target, String fieldName) throws ReflectiveOperationException {
        if (target == null) {
            throw new NullPointerException("target");
        }
        Field field = findField(target.getClass(), fieldName);
        if (field == null) {
            throw new NoSuchFieldException(target.getClass().getName() + "." + fieldName);
        }
        return field.get(target);
    }

    public static int methodCacheSize() {
        return METHOD_CACHE.size();
    }

    private static Optional<Class<?>> resolveClass(String className) {
        try {
            return Optional.of(Class.forName(className));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Method> resolveMethod(MethodKey key) {
        Object[] signature = signatureArguments(key.argumentTypes());
        for (Method method : key.owner().getMethods()) {
            if (methodMatches(method, key.methodName(), signature)) {
                prepareAccessible(method);
                return Optional.of(method);
            }
        }
        Method declared = findDeclaredMethod(key.owner(), key.methodName(), signature, new HashSet<>());
        if (declared != null) {
            prepareAccessible(declared);
            return Optional.of(declared);
        }
        return Optional.empty();
    }

    private static Optional<Constructor<?>> resolveConstructor(ConstructorKey key) {
        Object[] signature = signatureArguments(key.argumentTypes());
        for (Constructor<?> constructor : key.owner().getConstructors()) {
            if (parametersMatch(constructor.getParameterTypes(), signature)) {
                prepareAccessible(constructor);
                return Optional.of(constructor);
            }
        }
        for (Constructor<?> constructor : key.owner().getDeclaredConstructors()) {
            if (parametersMatch(constructor.getParameterTypes(), signature)) {
                prepareAccessible(constructor);
                return Optional.of(constructor);
            }
        }
        return Optional.empty();
    }

    private static Optional<Field> resolveField(FieldKey key) {
        Class<?> current = key.owner();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(key.fieldName());
                prepareAccessible(field);
                return Optional.of(field);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (Throwable ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static Method findDeclaredMethod(Class<?> type, String methodName, Object[] signature, Set<Class<?>> visited) {
        if (type == null || !visited.add(type)) {
            return null;
        }
        for (Method method : type.getDeclaredMethods()) {
            if (methodMatches(method, methodName, signature)) {
                return method;
            }
        }
        for (Class<?> iface : type.getInterfaces()) {
            Method method = findDeclaredMethod(iface, methodName, signature, visited);
            if (method != null) {
                return method;
            }
        }
        return findDeclaredMethod(type.getSuperclass(), methodName, signature, visited);
    }

    private static boolean methodMatches(Method method, String methodName, Object[] signature) {
        return method.getName().equals(methodName) && parametersMatch(method.getParameterTypes(), signature);
    }

    private static boolean parametersMatch(Class<?>[] parameterTypes, Object[] signature) {
        if (parameterTypes.length != signature.length) {
            return false;
        }
        for (int index = 0; index < parameterTypes.length; index++) {
            Object argumentType = signature[index];
            if (argumentType == NullArgument.INSTANCE) {
                if (parameterTypes[index].isPrimitive()) {
                    return false;
                }
                continue;
            }
            Class<?> actualType = (Class<?>) argumentType;
            if (!wrap(parameterTypes[index]).isAssignableFrom(actualType)) {
                return false;
            }
        }
        return true;
    }

    private static List<Class<?>> argumentTypes(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return List.of();
        }
        List<Class<?>> types = new ArrayList<>(arguments.length);
        for (Object argument : arguments) {
            types.add(argument == null ? NullArgument.class : argument.getClass());
        }
        return List.copyOf(types);
    }

    private static Object[] signatureArguments(List<Class<?>> argumentTypes) {
        Object[] signature = new Object[argumentTypes.size()];
        for (int index = 0; index < argumentTypes.size(); index++) {
            Class<?> type = argumentTypes.get(index);
            signature[index] = type == NullArgument.class ? NullArgument.INSTANCE : type;
        }
        return signature;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private static void prepareAccessible(java.lang.reflect.AccessibleObject member) {
        try {
            member.setAccessible(true);
        } catch (Throwable ignored) {
            // Public members can still be invoked when strong encapsulation rejects this call.
        }
    }

    private record MethodKey(Class<?> owner, String methodName, List<Class<?>> argumentTypes) {
    }

    private record ConstructorKey(Class<?> owner, List<Class<?>> argumentTypes) {
    }

    private record FieldKey(Class<?> owner, String fieldName) {
    }

    private enum NullArgument {
        INSTANCE
    }
}
