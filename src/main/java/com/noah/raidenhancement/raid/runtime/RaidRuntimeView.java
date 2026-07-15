package com.noah.raidenhancement.raid.runtime;

/**
 * Narrow read-only boundary for consumers that need access to the active raid runtime.
 *
 * <p>The native raid remains an opaque handle during the compatibility transition. Keeping
 * it behind this interface prevents presentation modules from reflecting into controller-private
 * state while allowing the existing, tested native-raid compatibility code to remain unchanged.
 * Implementations are the existing runtime objects, so hot BossBar paths allocate no view objects.</p>
 */
public interface RaidRuntimeView {
    String key();

    Object nativeRaidHandle();

    boolean completed();

    default boolean active() {
        return !completed();
    }
}
