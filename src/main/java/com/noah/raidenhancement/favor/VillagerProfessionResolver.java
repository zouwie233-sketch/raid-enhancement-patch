package com.noah.raidenhancement.favor;

import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;
import java.util.Locale;

/** Resolves the current server-side villager profession and trade level at interaction time. */
public final class VillagerProfessionResolver {
    private VillagerProfessionResolver() {
    }

    public static ProfessionInfo resolve(Entity villager) {
        if (villager == null) {
            return new ProfessionInfo("generic", 1, false);
        }
        int level = 1;
        String rawProfession = "";
        try {
            Object data = call(villager, "getVillagerData");
            if (data != null) {
                Object profession = call(data, "getProfession");
                rawProfession = profession == null ? "" : profession.toString();
                Object levelObject = call(data, "getLevel");
                if (levelObject instanceof Number number) {
                    level = number.intValue();
                }
            }
        } catch (Throwable ignored) {
        }
        level = Math.max(1, Math.min(5, level));
        String group = group(rawProfession);
        return new ProfessionInfo(group, level, !"generic".equals(group));
    }

    private static Object call(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String group(String raw) {
        String text = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        if (text.contains("farmer")) return "farmer";
        if (text.contains("librarian")) return "librarian";
        if (text.contains("shepherd")) return "shepherd";
        if (text.contains("fisherman")) return "fisherman";
        if (text.contains("fletcher")) return "fletcher";
        if (text.contains("cleric")) return "cleric";
        if (text.contains("mason")) return "mason";
        if (text.contains("toolsmith")) return "toolsmith";
        if (text.contains("weaponsmith")) return "weaponsmith";
        if (text.contains("armorer")) return "armorer";
        if (text.contains("leatherworker")) return "leatherworker";
        if (text.contains("cartographer")) return "cartographer";
        if (text.contains("butcher")) return "butcher";
        return "generic";
    }

    public record ProfessionInfo(String group, int villagerLevel, boolean knownProfession) {
    }
}
