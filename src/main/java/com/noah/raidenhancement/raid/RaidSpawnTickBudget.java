package com.noah.raidenhancement.raid;

import java.util.Map;
import java.util.WeakHashMap;

/** Shared per-level/per-game-time admission budget for all active raid spawn queues. */
public final class RaidSpawnTickBudget {
    private static final Map<Object, Budget> BUDGETS = new WeakHashMap<>();

    private RaidSpawnTickBudget() {
    }

    public static int claim(Object levelKey, long gameTime, int requested, int globalLimit) {
        if (levelKey == null || requested <= 0 || globalLimit <= 0) {
            return 0;
        }
        Budget budget = BUDGETS.computeIfAbsent(levelKey, ignored -> new Budget());
        if (budget.gameTime != gameTime) {
            budget.gameTime = gameTime;
            budget.remaining = Math.max(1, globalLimit);
        }
        int granted = Math.min(Math.max(0, requested), Math.max(0, budget.remaining));
        budget.remaining -= granted;
        return granted;
    }

    private static final class Budget {
        private long gameTime = Long.MIN_VALUE;
        private int remaining;
    }
}
