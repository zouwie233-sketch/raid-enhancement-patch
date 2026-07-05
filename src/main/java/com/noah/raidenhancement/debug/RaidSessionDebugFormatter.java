package com.noah.raidenhancement.debug;

import com.noah.raidenhancement.raid.AttackPoint;
import com.noah.raidenhancement.raid.RaidSession;

import java.util.stream.Collectors;

/** Debug-only formatter for phase reports and future log messages. */
public final class RaidSessionDebugFormatter {
    private RaidSessionDebugFormatter() {
    }

    public static String describe(RaidSession session) {
        if (session == null) {
            return "RaidSession{null}";
        }
        String points = session.currentWavePlan()
                .map(plan -> plan.attackPoints().stream().map(AttackPoint::compact).collect(Collectors.joining("; ")))
                .orElse("no-wave-plan");
        return session.shortSummary() + " attackPoints=[" + points + "]";
    }
}
