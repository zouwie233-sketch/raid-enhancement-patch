package com.noah.raidenhancement.favor;

import com.noah.raidenhancement.compat.ComponentCompat;
import com.noah.raidenhancement.config.VillageFavorConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** User-facing feedback only. It never decides eligibility or rewards. */
public final class FavorDisplay {
    private FavorDisplay() {
    }

    public static void sendVictoryRecorded(Player player, VillageFavorRecord record) {
        if (player == null || record == null) {
            return;
        }
        sendMessage(player, "[村庄恩情] 本村村民记下了你的防卫功绩。当前恩情等级："
                + levelName(record.favorLevel) + "，胜利次数：" + record.victoryCount + "，贡献分：" + record.raidMeritScore + "。");
    }

    public static void sendGreeting(Player player, VillageFavorRecord record, boolean giftGiven) {
        if (player == null || record == null || !VillageFavorConfig.ENABLE_GREETING) {
            return;
        }
        String suffix = giftGiven ? "村民还送上了一份小礼物。" : "";
        sendMessage(player, "[村庄恩情] 这里的村民认出了你：" + levelName(record.favorLevel)
                + "。" + suffix);
    }

    public static void playInteractionFeedback(ServerLevel level, Player player, Entity villager) {
        if (level == null || player == null || villager == null) {
            return;
        }
        if (VillageFavorConfig.ENABLE_SOUND) {
            playSound(level, player, villager);
        }
        if (VillageFavorConfig.ENABLE_PARTICLES) {
            spawnParticles(level, villager);
        }
    }

    public static String levelName(int level) {
        return switch (Math.max(0, Math.min(VillageFavorConfig.MAX_FAVOR_LEVEL, level))) {
            case 1 -> "记名恩人";
            case 2 -> "村庄友人";
            case 3 -> "防卫功臣";
            case 4 -> "守城英雄";
            case 5 -> "主城守护者";
            default -> "无";
        };
    }

    private static void sendMessage(Player player, String message) {
        try {
            Component component = ComponentCompat.literal(message);
            if (component != null) {
                player.sendSystemMessage(component);
            }
        } catch (Throwable ignored) {
            // Display must not affect real logic.
        }
    }

    private static void playSound(ServerLevel level, Player player, Entity villager) {
        try {
            Class<?> soundEventsClass = Class.forName("net.minecraft.sounds.SoundEvents");
            Object sound = null;
            for (String fieldName : new String[]{"VILLAGER_YES", "EXPERIENCE_ORB_PICKUP"}) {
                try {
                    Field field = soundEventsClass.getField(fieldName);
                    sound = field.get(null);
                    if (sound != null) {
                        break;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (sound == null) {
                return;
            }
            Object source = null;
            try {
                Class<?> sourceClass = Class.forName("net.minecraft.sounds.SoundSource");
                source = Enum.valueOf((Class<Enum>) sourceClass.asSubclass(Enum.class), "NEUTRAL");
            } catch (Throwable ignored) {
            }
            for (Method method : level.getClass().getMethods()) {
                if (!"playSound".equals(method.getName()) || method.getParameterCount() < 6) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                Object[] args = new Object[method.getParameterCount()];
                args[0] = player;
                args[1] = villager.getX();
                args[2] = villager.getY();
                args[3] = villager.getZ();
                args[4] = sound;
                args[5] = source;
                if (args.length > 6) {
                    args[6] = 0.65F;
                }
                if (args.length > 7) {
                    args[7] = 1.1F;
                }
                method.invoke(level, args);
                return;
            }
        } catch (Throwable ignored) {
        }
    }

    private static void spawnParticles(ServerLevel level, Entity villager) {
        try {
            Class<?> particleTypesClass = Class.forName("net.minecraft.core.particles.ParticleTypes");
            Object particle = null;
            for (String fieldName : new String[]{"HAPPY_VILLAGER", "HEART"}) {
                try {
                    Field field = particleTypesClass.getField(fieldName);
                    particle = field.get(null);
                    if (particle != null) {
                        break;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (particle == null) {
                return;
            }
            for (Method method : level.getClass().getMethods()) {
                if (!"sendParticles".equals(method.getName()) || method.getParameterCount() < 7) {
                    continue;
                }
                Object[] args = new Object[method.getParameterCount()];
                args[0] = particle;
                args[1] = villager.getX();
                args[2] = villager.getY() + 1.2D;
                args[3] = villager.getZ();
                args[4] = 5;
                args[5] = 0.35D;
                args[6] = 0.35D;
                if (args.length > 7) {
                    args[7] = 0.35D;
                }
                if (args.length > 8) {
                    args[8] = 0.02D;
                }
                method.invoke(level, args);
                return;
            }
        } catch (Throwable ignored) {
        }
    }
}
