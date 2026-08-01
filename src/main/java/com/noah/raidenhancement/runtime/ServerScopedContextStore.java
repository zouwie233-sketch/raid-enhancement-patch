package com.noah.raidenhancement.runtime;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Identity-keyed lifecycle store used to bind runtime state to one concrete server instance.
 *
 * <p>This class is deliberately Minecraft-independent so its identity, removal and idempotent
 * close contracts can be tested with the JDK alone.</p>
 */
public final class ServerScopedContextStore<K, V extends AutoCloseable> implements AutoCloseable {
    private final Map<K, V> contexts = new IdentityHashMap<>();
    private final Function<K, V> factory;

    public ServerScopedContextStore(Function<K, V> factory) {
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    public synchronized V getOrCreate(K key) {
        Objects.requireNonNull(key, "key");
        V existing = contexts.get(key);
        if (existing != null) {
            return existing;
        }
        V created = Objects.requireNonNull(factory.apply(key), "factory result");
        contexts.put(key, created);
        return created;
    }

    public synchronized Optional<V> find(K key) {
        return key == null ? Optional.empty() : Optional.ofNullable(contexts.get(key));
    }

    public boolean closeAndRemove(K key) {
        V removed;
        synchronized (this) {
            removed = key == null ? null : contexts.remove(key);
        }
        if (removed == null) {
            return false;
        }
        closeContext(removed);
        return true;
    }

    public synchronized int size() {
        return contexts.size();
    }

    @Override
    public void close() {
        List<V> removed;
        synchronized (this) {
            removed = new ArrayList<>(contexts.values());
            contexts.clear();
        }
        IllegalStateException failure = null;
        for (V context : removed) {
            try {
                context.close();
            } catch (Exception exception) {
                if (failure == null) {
                    failure = new IllegalStateException("Failed to close one or more server-scoped contexts");
                }
                failure.addSuppressed(exception);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static void closeContext(AutoCloseable context) {
        try {
            context.close();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to close server-scoped context", exception);
        }
    }
}
