package com.noah.raidenhancement.raid;

import com.noah.raidenhancement.compat.MobEffectCompat;
import com.noah.raidenhancement.config.RaidEnhancementConfig;
import com.noah.raidenhancement.raid.runtime.VillageSecurityRuntimeView;
import com.noah.raidenhancement.villager.VillagerProtectionController;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Step 8.9.6: configurable village security deployment-doctrine layer.
 *
 * This is a conservative replacement for the earlier idea of hard-replacing
 * vanilla raid failure. Step 8.9.3 also converts clear-time overruns
 * into controlled village pressure through the existing security system. It does not set vanilla Raid loss/victory, does not
 * clear raiders, and does not teleport anything. It only creates temporary
 * managed iron golems that represent the village security line. If all managed
 * security golems for the current logical wave die, the village suffers one
 * controlled breach-damage event for that wave.
 */
public final class VillageSecurityController {
    private static final String AABB_CLASS_NAME = "net.minecraft.world.phys.AABB";
    private static final String IRON_GOLEM_CLASS_NAME = "net.minecraft.world.entity.animal.IronGolem";
    private static final String IRON_GOLEM_ID = "minecraft:iron_golem";
    private static final Map<String, SecuritySession> SESSIONS = new LinkedHashMap<>();
    private static boolean announced;
    private static boolean warnedTickFailure;
    private static boolean warnedSpawnFailure;
    private static boolean warnedCleanupFailure;
    private static boolean warnedHealthFailure;
    private static boolean warnedGolemTrimFailure;

    private static final String[] INITIAL_ENTRY_MESSAGES = {
            "[村民防卫同盟] 袭击警报确认。普通雇佣军已撤出战场，村庄进入战役防卫状态。",
            "[村民防卫同盟] 村庄遭遇战役级袭击，日常雇佣军已不再适合守卫前线。",
            "[村民防卫同盟] 战役防卫协议启动。普通雇佣军撤离，安防铁傀儡即将接管防线。",
            "[村民防卫同盟] 袭击规模已超出日常防卫范围，村民防卫同盟开始介入。",
            "[村民防卫同盟] 第一波袭击来临，村庄防线由村民防卫同盟正式接管。"
    };

    private static final String[] INITIAL_DEPLOYMENT_MESSAGES = {
            "[村民防卫同盟] 第一批安防铁傀儡已抵达，当前防卫力量：%s 名。",
            "[村民防卫同盟] %s 名安防铁傀儡已进入村庄防线。",
            "[村民防卫同盟] 战役级安防力量部署完成，当前可用铁傀儡：%s 名。",
            "[村民防卫同盟] 同盟支援部队已入场，安防铁傀儡数量：%s 名。"
    };

    private static final String[] REINFORCEMENT_MESSAGES = {
            "[村民防卫同盟] 第 %s 波袭击来临。上一轮防线受损，已补充 %s 名安防铁傀儡，当前防卫力量：%s/%s。",
            "[村民防卫同盟] 第 %s 波开始。村庄防线出现缺口，增援完成：新增 %s 名，当前守卫：%s/%s。",
            "[村民防卫同盟] 第 %s 波袭击接近。安防力量不足，村民防卫同盟已派遣 %s 名铁傀儡补充阵位。",
            "[村民防卫同盟] 第 %s 波战斗即将开始，新增 %s 名安防铁傀儡，当前防卫力量：%s/%s。",
            "[村民防卫同盟] 第 %s 波来袭。防卫同盟已重新补足村庄战役守卫力量，增援数量：%s 名。"
    };

    private static final String[] REPAIR_MESSAGES = {
            "[村民防卫同盟] 第 %s 波袭击来临。%s 名安防铁傀儡已完成战地修复，防线继续维持。",
            "[村民防卫同盟] 第 %s 波开始。安防铁傀儡完成整备，村庄防线保持完整。",
            "[村民防卫同盟] 第 %s 波即将接战。无需新增铁傀儡，现有防线已完成修复。",
            "[村民防卫同盟] 第 %s 波袭击接近，当前 %s 名安防铁傀儡已恢复作战能力。",
            "[村民防卫同盟] 第 %s 波来袭。村庄防线未出现缺员，战地修复已完成。"
    };

    private static final String[] WAVE_FAILED_MESSAGES = {
            "[村民防卫同盟] 第 %s 波防守失败，安防铁傀儡已全数失去作战能力。",
            "[村民防卫同盟] 第 %s 波村庄防线被突破，安防铁傀儡已无法继续作战。",
            "[村民防卫同盟] 第 %s 波防线崩溃，村民将承受战役压力。",
            "[村民防卫同盟] 第 %s 波安防力量全灭，村庄内部防线受到冲击。",
            "[村民防卫同盟] 第 %s 波守卫失败，袭击者突破了安防铁傀儡防线。"
    };

    private static final String[] VILLAGER_PENALTY_MESSAGES = {
            "[村民防卫同盟] 村民防线被突破，村民生命值降低 %s 点。",
            "[村民防卫同盟] 战线崩溃造成村民伤亡压力，村民生命值降低 %s 点。",
            "[村民防卫同盟] 安防防线失守，村民受到冲击，生命值降低 %s 点。",
            "[村民防卫同盟] 村庄承受防守失败代价，村民生命值降低 %s 点。"
    };

    private static final String[] VICTORY_MESSAGES = {
            "[村民防卫同盟] 袭击已被击退，村庄防卫战胜利。",
            "[村民防卫同盟] 村庄成功守住了本轮袭击，战役防卫状态解除。",
            "[村民防卫同盟] 袭击者已溃散，村庄防线完成防卫任务。",
            "[村民防卫同盟] 村庄防卫战结束，胜利属于幸存的村民。",
            "[村民防卫同盟] 战役级袭击已结束，村庄安全状态恢复。"
    };

    private static final String[] PERFECT_VICTORY_MESSAGES = {
            "[村民防卫同盟] 完胜：安防铁傀儡防线全程未被突破，村庄完成高质量防卫。",
            "[村民防卫同盟] 完胜判定：本次袭击中没有任何一波安防防线崩溃。",
            "[村民防卫同盟] 完胜：村庄战役防线保持完整，村民防卫同盟完成无崩溃支援。",
            "[村民防卫同盟] 完胜记录已生成，安防系统未检测到防守失败波次。"
    };

    private static final String[] NORMAL_VICTORY_MESSAGES = {
            "[村民防卫同盟] 胜利：村庄守住了袭击，但防线承受了一定损耗。",
            "[村民防卫同盟] 胜利判定：袭击已被击退，防线压力记录处于可接受范围。",
            "[村民防卫同盟] 胜利：敌军溃散，村庄仍保持基本防卫能力。",
            "[村民防卫同盟] 胜利记录已生成，村庄完成战役防卫。"
    };

    private static final String[] COSTLY_VICTORY_MESSAGES = {
            "[村民防卫同盟] 惨胜：袭击虽被击退，但村庄经历了 %s 次防线崩溃与 %s 次清剿超时。",
            "[村民防卫同盟] 惨胜判定：村庄守住了袭击，但安防防线崩溃 %s 次，清剿超时 %s 次。",
            "[村民防卫同盟] 惨胜：敌军最终溃散，村庄承受了 %s 次防守失败与 %s 次超时压力。",
            "[村民防卫同盟] 惨胜记录已生成，防线突破次数：%s，清剿超时次数：%s。"
    };

    private static final String[] VICTORY_SUMMARY_MESSAGES = {
            "[村民防卫同盟] 战后总结：共整备 %s 个波次，防守失败 %s 次，村民受冲击 %s 人次，回收临时安防铁傀儡 %s 名，普通雇佣军返回 %s 名。",
            "[村民防卫同盟] 战役报告：波次 %s，防线崩溃 %s 次，累计村民生命惩罚对象 %s 人次，撤离安防铁傀儡 %s 名，恢复日常守卫 %s 名。",
            "[村民防卫同盟] 战后统计：处理波次 %s，防守失败 %s，村民受损记录 %s，临时守卫回收 %s，日常雇佣军补回 %s。"
    };

    private static final String[] FAILURE_SUMMARY_MESSAGES = {
            "[村民防卫同盟] 战后总结：防卫至第 %s 波，累计防守失败 %s 次，村民受冲击 %s 人次，回收临时安防铁傀儡 %s 名。",
            "[村民防卫同盟] 失败报告：最高接战波次 %s，防线崩溃 %s 次，村民受损记录 %s，临时安防力量清理 %s 名。",
            "[村民防卫同盟] 战役统计：结束波次 %s，防守失败 %s，村民生命惩罚对象 %s 人次，撤离临时守卫 %s。"
    };

    private static final String[] VICTORY_SUMMARY_TIMEOUT_MESSAGES = {
            "[村民防卫同盟] 战后总结：共整备 %s 个波次，防守失败 %s 次，清剿超时 %s 次，防线崩溃受冲击 %s 人次，超时受冲击 %s 人次，回收临时安防铁傀儡 %s 名，普通雇佣军返回 %s 名。",
            "[村民防卫同盟] 战役报告：波次 %s，防线崩溃 %s 次，清剿超时 %s 次，防线冲击 %s 人次，超时冲击 %s 人次，撤离安防铁傀儡 %s 名，恢复日常守卫 %s 名。",
            "[村民防卫同盟] 战后统计：处理波次 %s，防守失败 %s，清剿超时 %s，防线受损记录 %s，超时受损记录 %s，临时守卫回收 %s，日常雇佣军补回 %s。"
    };

    private static final String[] FAILURE_SUMMARY_TIMEOUT_MESSAGES = {
            "[村民防卫同盟] 战后总结：防卫至第 %s 波，累计防守失败 %s 次，清剿超时 %s 次，防线崩溃受冲击 %s 人次，超时受冲击 %s 人次，回收临时安防铁傀儡 %s 名。",
            "[村民防卫同盟] 失败报告：最高接战波次 %s，防线崩溃 %s 次，清剿超时 %s 次，防线受损记录 %s，超时受损记录 %s，临时安防力量清理 %s 名。",
            "[村民防卫同盟] 战役统计：结束波次 %s，防守失败 %s，清剿超时 %s，防线生命惩罚对象 %s 人次，超时生命惩罚对象 %s 人次，撤离临时守卫 %s。"
    };

    private static final String[] SECURITY_WITHDRAW_MESSAGES = {
            "[村民防卫同盟] 临时安防铁傀儡已撤离，村庄恢复日常防卫状态。",
            "[村民防卫同盟] 战役守卫任务完成，安防铁傀儡开始撤出村庄。",
            "[村民防卫同盟] 村民防卫同盟已结束本次支援，临时安防力量撤离战场。",
            "[村民防卫同盟] 安防铁傀儡完成任务，村庄防线交还日常守卫力量。"
    };

    private static final String[] MERCENARY_RETURN_MESSAGES = {
            "[村民防卫同盟] 普通雇佣军返回村庄岗位，继续负责日常防卫。",
            "[村民防卫同盟] 日常防卫力量已恢复，普通雇佣军重新接管村庄巡逻。",
            "[村民防卫同盟] 战役结束，普通雇佣军已返回村庄防卫序列。",
            "[村民防卫同盟] 村庄恢复和平戒备状态，普通雇佣军重新上岗。"
    };

    private static final String[] FAILURE_MESSAGES = {
            "[村民防卫同盟] 村庄防卫战失败，安防系统已退出战场。",
            "[村民防卫同盟] 村庄防线失守，本次战役防卫未能完成。",
            "[村民防卫同盟] 袭击结果判定失败，村民防卫同盟终止本次支援。",
            "[村民防卫同盟] 村庄未能守住袭击，战役防卫状态结束。",
            "[村民防卫同盟] 本次村庄防卫失败，幸存者需要自行恢复。"
    };

    private static final String[] FAILURE_NO_HEAL_MESSAGES = {
            "[村民防卫同盟] 由于防卫失败，幸存村民不会获得战后治疗。",
            "[村民防卫同盟] 村庄未完成防卫目标，战后治疗程序未启动。",
            "[村民防卫同盟] 防卫失败，村民生命状态将保留当前损伤。",
            "[村民防卫同盟] 战役支援结束，幸存村民不会获得胜利恢复。"
    };

    private static final String[] TIMEOUT_PENALTY_MESSAGES = {
            "[村民防卫同盟] 第 %s 波清剿超时，袭击者正在拖垮村庄防线。",
            "[村民防卫同盟] 第 %s 波清剿未能按时完成，村民开始承受战役压力。",
            "[村民防卫同盟] 第 %s 波战斗拖延过久，村庄内部防线受到影响。",
            "[村民防卫同盟] 第 %s 波袭击者未能及时肃清，村民防卫同盟判定清剿超时。",
            "[村民防卫同盟] 第 %s 波清剿失败，村庄防卫压力上升。"
    };

    private static final String[] TIMEOUT_VILLAGER_PENALTY_MESSAGES = {
            "[村民防卫同盟] 清剿超时造成村民疲惫，村民生命值降低 %s 点。",
            "[村民防卫同盟] 战斗拖延造成村庄损耗，村民生命值降低 %s 点。",
            "[村民防卫同盟] 袭击者持续压迫村庄，村民生命值降低 %s 点。",
            "[村民防卫同盟] 清剿超时转化为村庄压力，村民生命值降低 %s 点。"
    };

    private static final String[] REDEPLOYMENT_MESSAGES = {
            "[村民防卫同盟] 第 %s 波接战前，%s 名安防铁傀儡已回撤并重新部署至村庄中心防线。",
            "[村民防卫同盟] 第 %s 波防御部署完成，%s 名安防铁傀儡已重整阵位。",
            "[村民防卫同盟] 第 %s 波来袭前，同盟正规军完成防线重组，重新部署单位：%s 名。",
            "[村民防卫同盟] 第 %s 波战场校正完成，%s 名安防铁傀儡已回到防御阵位。"
    };

    private static final String[] SURVIVOR_REPAIR_MESSAGES = {
            "[村民防卫同盟] 上一波存活的 %s 名安防铁傀儡已完成生命恢复，继续执行正规防卫任务。",
            "[村民防卫同盟] %s 名存活安防铁傀儡完成战地恢复，进入下一波作战序列。",
            "[村民防卫同盟] 同盟正规军完成波次间整备，存活单位恢复数量：%s 名。",
            "[村民防卫同盟] 安防铁傀儡战后维护完成，%s 名存活单位恢复作战状态。"
    };

    private static final String[] COMBAT_BUFF_MESSAGES = {
            "[村民防卫同盟] 同盟正规军防护协议启动，安防铁傀儡获得抗性 %s 级。",
            "[村民防卫同盟] 正规军装甲加护已生效，安防铁傀儡获得抗性 %s 级。",
            "[村民防卫同盟] 为区分普通雇佣军，安防铁傀儡已接入战役级抗性 %s 级。",
            "[村民防卫同盟] 村民防卫同盟已部署正规军强化协议：抗性 %s 级。"
    };

    private VillageSecurityController() {
    }

    /**
     * Exposes only the immutable session data needed by battle-support items.
     * Session ownership and mutation remain private to this controller.
     */
    public static List<VillageSecurityRuntimeView> runtimeViews() {
        List<VillageSecurityRuntimeView> views = new ArrayList<>(SESSIONS.size());
        for (SecuritySession session : SESSIONS.values()) {
            if (session == null) {
                continue;
            }
            views.add(new VillageSecurityRuntimeView(session.raidKey, session.dimensionId,
                    session.centerX, session.centerY, session.centerZ, session.securityGolemIds));
        }
        return List.copyOf(views);
    }

    public static void tick(ServerLevel level, String raidKey, String dimensionId,
                            int centerX, int centerY, int centerZ,
                            String difficultyName, int logicalWave, int totalWaves,
                            long gameTime, boolean activeCombatPlayerPresent) {
        if (!RaidEnhancementConfig.VILLAGE_SECURITY_ENABLED || level == null || raidKey == null || raidKey.isBlank()) {
            return;
        }
        try {
            if (!announced) {
                announced = true;
                System.out.println("[Raid Enhancement Patch] Village security 0.8.9.6.1 deployment doctrine hotfix is active. Resistance buffs are applied visibly after entity insertion and survivor repair is enforced at wave start.");
            }
            SecuritySession session = SESSIONS.computeIfAbsent(raidKey, key -> new SecuritySession(key, dimensionId, difficultyName));
            session.dimensionId = dimensionId;
            if (session.difficultyName == null || session.difficultyName.isBlank()) {
                session.difficultyName = difficultyName;
            }
            session.centerX = centerX;
            session.centerY = centerY;
            session.centerZ = centerZ;
            session.lastSeenGameTime = gameTime;

            if (!activeCombatPlayerPresent) {
                // Match the active-combat policy: while nobody is fighting, do
                // not initialize a new security wave and do not treat unloaded golems
                // as dead. Existing managed golems will be reconciled when players return.
                session.lastNoActiveCombatGameTime = gameTime;
                session.allSecurityGolemsMissingSinceGameTime = 0L;
                return;
            }

            if (!session.ordinaryGolemTrimmed) {
                trimOrdinaryGolemsAtRaidStart(level, session);
                session.ordinaryGolemTrimmed = true;
            }

            int wave = Math.max(1, Math.min(Math.max(1, logicalWave), Math.max(1, totalWaves)));
            if (session.currentWave != wave) {
                beginWave(level, session, wave, gameTime);
                return;
            }

            if (session.lastTickGameTime > 0L
                    && gameTime - session.lastTickGameTime < Math.max(1, RaidEnhancementConfig.VILLAGE_SECURITY_TICK_INTERVAL_TICKS)) {
                return;
            }
            session.lastTickGameTime = gameTime;
            maintainActiveWave(level, session, gameTime);
        } catch (Throwable throwable) {
            if (!warnedTickFailure) {
                warnedTickFailure = true;
                System.out.println("[Raid Enhancement Patch] Village security tick failed once and was suppressed: " + throwable);
            }
        }
    }

    public static void complete(ServerLevel level, String raidKey, String dimensionId,
                                int centerX, int centerY, int centerZ,
                                String difficultyName, boolean victory, long gameTime) {
        if (!RaidEnhancementConfig.VILLAGE_SECURITY_ENABLED || level == null || raidKey == null) {
            return;
        }
        try {
            SecuritySession session = SESSIONS.remove(raidKey);
            int cleanedSecurityGolems = 0;
            if (session != null) {
                cleanedSecurityGolems = cleanupSecurityGolems(level, session);
            }
            int radius = securityRadius();
            List<Villager> villagers = aliveVillagers(level, centerX, centerY, centerZ, radius);
            int releasedClamps = VillagerProtectionController.releaseVillageSecurityHealthClamps(level, villagers, true);
            int preparedWaves = session == null ? 0 : Math.max(0, session.wavesPrepared);
            int highestWave = session == null ? 0 : Math.max(0, session.highestPreparedWave);
            int defenseFailures = session == null ? 0 : Math.max(0, session.defenseFailureCount);
            int timeoutPenalties = session == null ? 0 : Math.max(0, session.timeoutPenaltyCount);
            int damagedVillagerEvents = session == null ? 0 : Math.max(0, session.totalBreachDamagedVillagers);
            int timeoutDamagedVillagerEvents = session == null ? 0 : Math.max(0, session.totalTimeoutDamagedVillagers);
            if (victory) {
                healVillagersToFull(villagers);
                int count = victoryGolemCount(difficultyName);
                int restored = 0;
                for (int i = 0; i < count; i++) {
                    int[] pos = ringPosition(level, centerX, centerY, centerZ, i, Math.max(1, count),
                            Math.max(8, RaidEnhancementConfig.VILLAGE_SECURITY_GOLEM_SPAWN_RING_RADIUS + 4));
                    if (spawnIronGolem(level, pos[0], pos[1], pos[2], false, null, i) != null) {
                        restored++;
                    }
                }
                if (RaidEnhancementConfig.VILLAGE_SECURITY_VICTORY_MESSAGES_ENABLED) {
                    if (RaidEnhancementConfig.VILLAGE_SECURITY_VICTORY_GRADE_MESSAGES_ENABLED) {
                        VictoryTier tier = VictoryTier.grade(defenseFailures, timeoutPenalties, damagedVillagerEvents, timeoutDamagedVillagerEvents);
                        if (tier == VictoryTier.PERFECT) {
                            sendSecurityMessage(level, centerX, centerY, centerZ, pick(PERFECT_VICTORY_MESSAGES));
                        } else if (tier == VictoryTier.COSTLY) {
                            sendSecurityMessage(level, centerX, centerY, centerZ,
                                    formatPick(COSTLY_VICTORY_MESSAGES, defenseFailures, timeoutPenalties));
                        } else {
                            sendSecurityMessage(level, centerX, centerY, centerZ, pick(NORMAL_VICTORY_MESSAGES));
                        }
                    } else {
                        sendSecurityMessage(level, centerX, centerY, centerZ, pick(VICTORY_MESSAGES));
                    }
                    sendSecurityMessage(level, centerX, centerY, centerZ, pick(SECURITY_WITHDRAW_MESSAGES));
                    if (restored > 0) {
                        sendSecurityMessage(level, centerX, centerY, centerZ, pick(MERCENARY_RETURN_MESSAGES));
                    }
                    if (RaidEnhancementConfig.VILLAGE_SECURITY_BATTLE_SUMMARY_MESSAGES_ENABLED) {
                        if (RaidEnhancementConfig.VILLAGE_SECURITY_TIMEOUT_PENALTY_INCLUDE_IN_BATTLE_SUMMARY) {
                            sendSecurityMessage(level, centerX, centerY, centerZ,
                                    formatPick(VICTORY_SUMMARY_TIMEOUT_MESSAGES, preparedWaves, defenseFailures,
                                            timeoutPenalties, damagedVillagerEvents, timeoutDamagedVillagerEvents,
                                            cleanedSecurityGolems, restored));
                        } else {
                            sendSecurityMessage(level, centerX, centerY, centerZ,
                                    formatPick(VICTORY_SUMMARY_MESSAGES, preparedWaves, defenseFailures,
                                            damagedVillagerEvents, cleanedSecurityGolems, restored));
                        }
                    }
                }
                RaidSession raidSession = RaidSessionManager.get(raidKey).orElse(null);
                int settlementOmenLevel = raidSession == null ? 1 : raidSession.omenLevel();
                int settlementTotalWaves = raidSession == null ? preparedWaves : raidSession.totalWaves();
                VictorySettlementController.onVillageRaidCompleted(level, raidKey, dimensionId, centerX, centerY, centerZ,
                        difficultyName, settlementOmenLevel, settlementTotalWaves, true, preparedWaves,
                        defenseFailures, timeoutPenalties, damagedVillagerEvents, timeoutDamagedVillagerEvents, gameTime);

                debugLog(RaidEnhancementConfig.VILLAGE_SECURITY_DEBUG_COMPLETION,
                        "Village security victory rewards applied: released " + releasedClamps
                                + " villager health clamp(s), cleaned " + cleanedSecurityGolems
                                + " temporary security golem(s), healed villagers, restored " + restored
                                + " ordinary village-defense iron golem(s), preparedWaves=" + preparedWaves
                                + ", defenseFailures=" + defenseFailures
                                + ", timeoutPenalties=" + timeoutPenalties + ".");
            } else {
                if (RaidEnhancementConfig.VILLAGE_SECURITY_FAILURE_MESSAGES_ENABLED) {
                    sendSecurityMessage(level, centerX, centerY, centerZ, pick(FAILURE_MESSAGES));
                    sendSecurityMessage(level, centerX, centerY, centerZ, pick(FAILURE_NO_HEAL_MESSAGES));
                    if (RaidEnhancementConfig.VILLAGE_SECURITY_BATTLE_SUMMARY_MESSAGES_ENABLED) {
                        if (RaidEnhancementConfig.VILLAGE_SECURITY_TIMEOUT_PENALTY_INCLUDE_IN_BATTLE_SUMMARY) {
                            sendSecurityMessage(level, centerX, centerY, centerZ,
                                    formatPick(FAILURE_SUMMARY_TIMEOUT_MESSAGES, highestWave, defenseFailures,
                                            timeoutPenalties, damagedVillagerEvents, timeoutDamagedVillagerEvents,
                                            cleanedSecurityGolems));
                        } else {
                            sendSecurityMessage(level, centerX, centerY, centerZ,
                                    formatPick(FAILURE_SUMMARY_MESSAGES, highestWave, defenseFailures,
                                            damagedVillagerEvents, cleanedSecurityGolems));
                        }
                    }
                }
                debugLog(RaidEnhancementConfig.VILLAGE_SECURITY_DEBUG_COMPLETION,
                        "Village security ended without victory: released " + releasedClamps
                                + " villager health clamp(s), cleaned " + cleanedSecurityGolems
                                + " temporary security golem(s), no victory healing applied, highestWave="
                                + highestWave + ", defenseFailures=" + defenseFailures
                                + ", timeoutPenalties=" + timeoutPenalties + ".");
            }
        } catch (Throwable throwable) {
            if (!warnedCleanupFailure) {
                warnedCleanupFailure = true;
                System.out.println("[Raid Enhancement Patch] Village security completion failed once and was suppressed: " + throwable);
            }
        }
    }

    private static void beginWave(ServerLevel level, SecuritySession session, int wave, long gameTime) {
        session.currentWave = wave;
        session.wavesPrepared++;
        session.highestPreparedWave = Math.max(session.highestPreparedWave, wave);
        session.waveDamageApplied = false;
        session.currentWaveTimeoutPenaltyApplied = false;
        session.lastTimeoutPenaltyCheckGameTime = 0L;
        session.hadSecurityGolemsThisWave = false;
        session.lastTickGameTime = gameTime;
        pruneTrackedGolems(level, session, gameTime);

        List<Villager> villagers = refreshTrackedVillagers(level, session, gameTime);
        for (Villager villager : villagers) {
            VillagerProtectionController.ensureVillageSecurityHealthClamp(villager,
                    RaidEnhancementConfig.VILLAGE_SECURITY_HEALTH_CLAMP_DURATION_TICKS);
        }
        int villagerCount = villagers.size();
        int target = targetSecurityGolems(session.difficultyName, villagerCount);
        List<Entity> aliveGolems = trackedSecurityGolems(level, session, gameTime);
        int redeployed = redeployExistingSecurityGolems(level, session, aliveGolems, wave, target);
        int repaired = 0;
        for (Entity golem : aliveGolems) {
            if (RaidEnhancementConfig.VILLAGE_SECURITY_REPAIR_SURVIVORS_ON_WAVE_START) {
                repairSecuritySurvivor(golem);
                repaired++;
            }
            applySecurityCombatEffects(golem);
        }
        session.lastWaveStartMaintenanceWave = wave;
        session.lastGolemGlowGameTime = gameTime;
        int missing = Math.max(0, target - aliveGolems.size());
        int spawned = 0;
        int spawnPointCount = Math.max(1, RaidEnhancementConfig.VILLAGE_SECURITY_GOLEM_SPAWN_POINT_COUNT);
        for (int i = 0; i < missing; i++) {
            int pointIndex = Math.floorMod(session.nextSpawnPointCursor + i, spawnPointCount);
            int[] pos = deploymentPosition(level, session, pointIndex,
                    Math.max(spawnPointCount, Math.max(target, missing)),
                    RaidEnhancementConfig.VILLAGE_SECURITY_GOLEM_SPAWN_RING_RADIUS,
                    wave, true);
            Entity golem = spawnIronGolem(level, pos[0], pos[1], pos[2], true, session, i);
            if (golem != null) {
                spawned++;
            }
        }
        if (missing > 0) {
            session.nextSpawnPointCursor = Math.floorMod(session.nextSpawnPointCursor + missing, spawnPointCount);
        }
        int active = aliveGolems.size() + spawned;
        session.hadSecurityGolemsThisWave = active > 0;
        session.totalRepairedSecurityGolems += Math.max(0, repaired);
        session.totalRedeployedSecurityGolems += Math.max(0, redeployed);
        session.totalSpawnedSecurityGolems += Math.max(0, spawned);
        announceWavePreparation(level, session, wave, repaired, spawned, redeployed, active, target);
        debugLog(RaidEnhancementConfig.VILLAGE_SECURITY_DEBUG_WAVE_PREPARATION,
                "Village security wave " + wave + " prepared: villagers="
                        + villagerCount + ", targetSecurityGolems=" + target
                        + ", redeployedSecurityGolems=" + redeployed
                        + ", repairedSecurityGolems=" + repaired + ", spawnedSecurityGolems="
                        + spawned + ", activeSecurityGolems=" + active + ".");
    }

    private static void maintainActiveWave(ServerLevel level, SecuritySession session, long gameTime) {
        List<Villager> villagers = trackedVillagers(level, session);
        int villagerRefreshInterval = RaidEnhancementConfig.VILLAGE_SECURITY_PERFORMANCE_OPTIMIZATION_ENABLED
                ? Math.max(20, RaidEnhancementConfig.VILLAGE_SECURITY_VILLAGER_CACHE_REFRESH_INTERVAL_TICKS)
                : Math.max(1, RaidEnhancementConfig.VILLAGE_SECURITY_TICK_INTERVAL_TICKS);
        if (villagers.isEmpty() || session.lastVillagerRefreshGameTime <= 0L
                || gameTime - session.lastVillagerRefreshGameTime >= villagerRefreshInterval) {
            villagers = refreshTrackedVillagers(level, session, gameTime);
        }
        for (Villager villager : villagers) {
            VillagerProtectionController.ensureVillageSecurityHealthClamp(villager,
                    RaidEnhancementConfig.VILLAGE_SECURITY_HEALTH_CLAMP_DURATION_TICKS);
            VillagerProtectionController.enforceVillageSecurityHealthClamp(villager);
        }
        List<Entity> aliveGolems = trackedSecurityGolems(level, session, gameTime);
        if (session.lastWaveStartMaintenanceWave != session.currentWave && !aliveGolems.isEmpty()) {
            for (Entity golem : aliveGolems) {
                if (RaidEnhancementConfig.VILLAGE_SECURITY_REPAIR_SURVIVORS_ON_WAVE_START) {
                    repairSecuritySurvivor(golem);
                }
                applySecurityCombatEffects(golem);
            }
            session.lastWaveStartMaintenanceWave = session.currentWave;
            session.lastGolemGlowGameTime = gameTime;
            debugLog(RaidEnhancementConfig.VILLAGE_SECURITY_DEBUG_WAVE_PREPARATION,
                    "Village security late wave-start maintenance applied for wave "
                            + session.currentWave + " to " + aliveGolems.size() + " golem(s).");
        }
        int glowRefreshInterval = RaidEnhancementConfig.VILLAGE_SECURITY_PERFORMANCE_OPTIMIZATION_ENABLED
                ? Math.max(20, RaidEnhancementConfig.VILLAGE_SECURITY_GOLEM_GLOW_REFRESH_INTERVAL_TICKS)
                : Math.max(20, RaidEnhancementConfig.VILLAGE_SECURITY_TICK_INTERVAL_TICKS);
        if (session.lastGolemGlowGameTime <= 0L || gameTime - session.lastGolemGlowGameTime >= glowRefreshInterval) {
            for (Entity golem : aliveGolems) {
                applySecurityCombatEffects(golem);
            }
            session.lastGolemGlowGameTime = gameTime;
        }
        if (!aliveGolems.isEmpty()) {
            session.allSecurityGolemsMissingSinceGameTime = 0L;
        }
        if (session.hadSecurityGolemsThisWave && !session.waveDamageApplied && aliveGolems.isEmpty()) {
            if (shouldDelaySecurityGolemMissingBreach(session, gameTime)) {
                debugLog(RaidEnhancementConfig.VILLAGE_SECURITY_DEBUG_BREACH,
                        "Village security golems are not currently loaded/visible on wave "
                                + session.currentWave + "; delaying breach judgement for return/reload reconciliation.");
            } else {
            int damaged = applyBreachDamage(level, session);
            session.waveDamageApplied = true;
            session.defenseFailureCount++;
            session.totalBreachDamageEvents++;
            session.totalBreachDamagedVillagers += Math.max(0, damaged);
            if (RaidEnhancementConfig.VILLAGE_SECURITY_WAVE_FAILURE_MESSAGES_ENABLED) {
                sendSecurityMessage(level, session.centerX, session.centerY, session.centerZ,
                        formatPick(WAVE_FAILED_MESSAGES, session.currentWave));
                sendSecurityMessage(level, session.centerX, session.centerY, session.centerZ,
                        formatPick(VILLAGER_PENALTY_MESSAGES, damageText(RaidEnhancementConfig.VILLAGE_SECURITY_BREACH_DAMAGE)));
            }
            debugLog(RaidEnhancementConfig.VILLAGE_SECURITY_DEBUG_BREACH,
                    "Village security breach on wave " + session.currentWave
                            + ": all managed security golems are down; applied "
                            + RaidEnhancementConfig.VILLAGE_SECURITY_BREACH_DAMAGE
                            + " damage to " + damaged + " villager(s); defenseFailures="
                            + session.defenseFailureCount + ".");
            }
        }
        applyTimeoutPenaltyIfNeeded(level, session, gameTime);
    }

    private static boolean shouldDelaySecurityGolemMissingBreach(SecuritySession session, long gameTime) {
        if (session == null) {
            return false;
        }
        long grace = Math.max(0L, RaidEnhancementConfig.PERFORMANCE_SECURITY_GOLEM_RETURN_GRACE_TICKS);
        if (grace <= 0L) {
            return false;
        }
        if (session.lastNoActiveCombatGameTime > 0L
                && gameTime - session.lastNoActiveCombatGameTime <= grace) {
            session.allSecurityGolemsMissingSinceGameTime = 0L;
            return true;
        }
        if (!session.missingSecurityGolemSinceGameTime.isEmpty()) {
            if (session.allSecurityGolemsMissingSinceGameTime <= 0L) {
                session.allSecurityGolemsMissingSinceGameTime = gameTime;
                return true;
            }
            return gameTime - session.allSecurityGolemsMissingSinceGameTime < grace;
        }
        return false;
    }

    private static void applyTimeoutPenaltyIfNeeded(ServerLevel level, SecuritySession session, long gameTime) {
        if (!RaidEnhancementConfig.VILLAGE_SECURITY_TIMEOUT_PENALTY_ENABLED || level == null || session == null) {
            return;
        }
        if (RaidEnhancementConfig.VILLAGE_SECURITY_TIMEOUT_PENALTY_ONCE_PER_WAVE
                && session.currentWaveTimeoutPenaltyApplied) {
            return;
        }
        if (session.lastTimeoutPenaltyCheckGameTime > 0L
                && gameTime - session.lastTimeoutPenaltyCheckGameTime
                < Math.max(10, RaidEnhancementConfig.PERFORMANCE_TIMEOUT_PENALTY_CHECK_INTERVAL_TICKS)) {
            return;
        }
        session.lastTimeoutPenaltyCheckGameTime = gameTime;

        Optional<RaidSession> raidSessionOptional = RaidSessionManager.get(session.raidKey);
        if (raidSessionOptional.isEmpty()) {
            return;
        }
        RaidSession raidSession = raidSessionOptional.get();
        if (raidSession.currentWave() != session.currentWave
                || !raidSession.currentWaveClearStarted()
                || !raidSession.currentWaveBudgetLocked()
                || !raidSession.currentWaveTimedOut()
                || raidSession.currentWaveClockPausedAfterGrace()
                || raidSession.isClosed()) {
            return;
        }
        if (RaidEnhancementConfig.VILLAGE_SECURITY_TIMEOUT_PENALTY_SKIP_IF_WAVE_DEFENSE_ALREADY_FAILED
                && session.waveDamageApplied) {
            return;
        }
        if (RaidEnhancementConfig.VILLAGE_SECURITY_TIMEOUT_PENALTY_REQUIRE_KNOWN_LIVING_RAIDERS
                && !hasKnownLivingRaider(raidSession)) {
            return;
        }
        int damaged = applyTimeoutPenaltyDamage(level, session);
        session.currentWaveTimeoutPenaltyApplied = true;
        session.timeoutPenaltyCount++;
        session.totalTimeoutDamagedVillagers += Math.max(0, damaged);
        if (RaidEnhancementConfig.VILLAGE_SECURITY_TIMEOUT_PENALTY_MESSAGES_ENABLED) {
            sendSecurityMessage(level, session.centerX, session.centerY, session.centerZ,
                    formatPick(TIMEOUT_PENALTY_MESSAGES, session.currentWave));
            sendSecurityMessage(level, session.centerX, session.centerY, session.centerZ,
                    formatPick(TIMEOUT_VILLAGER_PENALTY_MESSAGES,
                            damageText(RaidEnhancementConfig.VILLAGE_SECURITY_TIMEOUT_PENALTY_DAMAGE)));
        }
        debugLog(RaidEnhancementConfig.VILLAGE_SECURITY_DEBUG_TIMEOUT_PENALTY,
                "Village security timeout penalty on wave " + session.currentWave
                        + ": applied " + RaidEnhancementConfig.VILLAGE_SECURITY_TIMEOUT_PENALTY_DAMAGE
                        + " damage to " + damaged + " villager(s); timeoutPenalties="
                        + session.timeoutPenaltyCount + ".");
    }

    private static boolean hasKnownLivingRaider(RaidSession raidSession) {
        if (raidSession == null) {
            return false;
        }
        for (RaiderRecord record : raidSession.trackedRaiders()) {
            if (record != null && record.aliveWhenLastSeen()) {
                return true;
            }
        }
        return false;
    }

    private static List<Villager> penaltyVillagers(ServerLevel level, SecuritySession session) {
        if (level == null || session == null) {
            return List.of();
        }
        Map<UUID, Villager> villagers = new LinkedHashMap<>();
        for (Villager villager : trackedVillagers(level, session)) {
            if (villager != null && villager.isAlive()) {
                villagers.put(villager.getUUID(), villager);
            }
        }
        for (Villager villager : aliveVillagers(level, session.centerX, session.centerY, session.centerZ, penaltyRadius())) {
            if (villager != null && villager.isAlive()) {
                villagers.put(villager.getUUID(), villager);
            }
        }
        return new ArrayList<>(villagers.values());
    }

    private static int applyTimeoutPenaltyDamage(ServerLevel level, SecuritySession session) {
        int affected = 0;
        float damage = Math.max(0.0F, RaidEnhancementConfig.VILLAGE_SECURITY_TIMEOUT_PENALTY_DAMAGE);
        float minHealth = Math.max(0.0F, RaidEnhancementConfig.VILLAGE_SECURITY_TIMEOUT_PENALTY_MIN_VILLAGER_HEALTH);
        if (damage <= 0.0F) {
            return 0;
        }
        for (Villager villager : penaltyVillagers(level, session)) {
            VillagerProtectionController.ensureVillageSecurityHealthClamp(villager,
                    RaidEnhancementConfig.VILLAGE_SECURITY_HEALTH_CLAMP_DURATION_TICKS);
            if (applyDirectHealthDeltaWithFloor(villager, -damage, minHealth)) {
                VillagerProtectionController.updateVillageSecurityAllowedHealth(villager);
                affected++;
            }
        }
        return affected;
    }

    private static int applyBreachDamage(ServerLevel level, SecuritySession session) {
        int affected = 0;
        for (Villager villager : penaltyVillagers(level, session)) {
            VillagerProtectionController.ensureVillageSecurityHealthClamp(villager,
                    RaidEnhancementConfig.VILLAGE_SECURITY_HEALTH_CLAMP_DURATION_TICKS);
            if (applyDirectHealthDelta(villager, -Math.max(0.0F, RaidEnhancementConfig.VILLAGE_SECURITY_BREACH_DAMAGE))) {
                VillagerProtectionController.updateVillageSecurityAllowedHealth(villager);
                affected++;
            }
        }
        return affected;
    }

    private static boolean applyDirectHealthDelta(Object livingEntity, float delta) {
        try {
            float health = getFloatNoArg(livingEntity, "getHealth", 0.0F);
            if (health <= 0.0F) {
                return false;
            }
            float next = Math.max(0.0F, health + delta);
            return setHealth(livingEntity, next);
        } catch (Throwable throwable) {
            if (!warnedHealthFailure) {
                warnedHealthFailure = true;
                System.out.println("[Raid Enhancement Patch] Village security could not adjust villager health once: " + throwable);
            }
            return false;
        }
    }

    private static boolean applyDirectHealthDeltaWithFloor(Object livingEntity, float delta, float minHealth) {
        try {
            float health = getFloatNoArg(livingEntity, "getHealth", 0.0F);
            if (health <= 0.0F) {
                return false;
            }
            float floor = Math.max(0.0F, minHealth);
            float next = Math.max(floor, health + delta);
            if (next >= health - 0.001F) {
                return false;
            }
            return setHealth(livingEntity, next);
        } catch (Throwable throwable) {
            if (!warnedHealthFailure) {
                warnedHealthFailure = true;
                System.out.println("[Raid Enhancement Patch] Village security could not adjust villager health once: " + throwable);
            }
            return false;
        }
    }

    private static void healVillagersToFull(List<Villager> villagers) {
        for (Villager villager : villagers) {
            repairLivingToFull(villager);
        }
    }

    private static int targetSecurityGolems(String difficultyName, int villagerCount) {
        int divisor = villagersPerSecurityGolem(difficultyName);
        int byVillagers = (int) Math.ceil(Math.max(0, villagerCount) / (double) Math.max(1, divisor));
        return clamp(byVillagers,
                Math.max(1, RaidEnhancementConfig.VILLAGE_SECURITY_MIN_GOLEMS_PER_WAVE),
                Math.max(1, RaidEnhancementConfig.VILLAGE_SECURITY_MAX_GOLEMS_PER_WAVE));
    }

    private static int villagersPerSecurityGolem(String difficultyName) {
        String key = difficultyKey(difficultyName);
        if (key.contains("HARD")) {
            return Math.max(1, RaidEnhancementConfig.VILLAGE_SECURITY_HARD_VILLAGERS_PER_GOLEM);
        }
        if (key.contains("NORMAL")) {
            return Math.max(1, RaidEnhancementConfig.VILLAGE_SECURITY_NORMAL_VILLAGERS_PER_GOLEM);
        }
        return Math.max(1, RaidEnhancementConfig.VILLAGE_SECURITY_EASY_VILLAGERS_PER_GOLEM);
    }

    private static int victoryGolemCount(String difficultyName) {
        String key = difficultyKey(difficultyName);
        if (key.contains("HARD")) {
            return Math.max(0, RaidEnhancementConfig.VILLAGE_SECURITY_VICTORY_GOLEMS_HARD);
        }
        if (key.contains("NORMAL")) {
            return Math.max(0, RaidEnhancementConfig.VILLAGE_SECURITY_VICTORY_GOLEMS_NORMAL);
        }
        return Math.max(0, RaidEnhancementConfig.VILLAGE_SECURITY_VICTORY_GOLEMS_EASY);
    }

    private static List<Villager> aliveVillagers(ServerLevel level, int x, int y, int z, int radius) {
        List<Villager> villagers = new ArrayList<>();
        try {
            Class<?> villagerClass = Class.forName("net.minecraft.world.entity.npc.Villager");
            Object box = aabbAround(x, y, z, radius);
            Method getEntities = level.getClass().getMethod("getEntitiesOfClass", Class.class, box.getClass());
            Object result = getEntities.invoke(level, villagerClass, box);
            if (result instanceof List<?> list) {
                for (Object candidate : list) {
                    if (candidate instanceof Villager villager && villager.isAlive()) {
                        villagers.add(villager);
                    }
                }
            }
        } catch (Throwable ignored) {
            return List.of();
        }
        return villagers;
    }

    private static void trimOrdinaryGolemsAtRaidStart(ServerLevel level, SecuritySession session) {
        if (!RaidEnhancementConfig.VILLAGE_SECURITY_TRIM_ORDINARY_GOLEMS_ON_RAID_START || level == null || session == null) {
            return;
        }
        try {
            int keep = ordinaryGolemKeepLimit(session.difficultyName);
            Set<UUID> securityIds = new HashSet<>(session.securityGolemIds);
            List<Entity> ordinary = new ArrayList<>();
            for (Entity golem : ironGolemsNear(level, session.centerX, session.centerY, session.centerZ, securityRadius())) {
                if (golem == null || !golem.isAlive() || securityIds.contains(golem.getUUID()) || hasCustomName(golem)) {
                    continue;
                }
                ordinary.add(golem);
            }
            ordinary.sort(Comparator.comparingDouble(golem -> distanceSquared(golem, session.centerX, session.centerY, session.centerZ)));
            int removed = 0;
            for (int i = Math.max(0, keep); i < ordinary.size(); i++) {
                discard(ordinary.get(i));
                removed++;
            }
            if (removed > 0) {
                debugLog(RaidEnhancementConfig.VILLAGE_SECURITY_DEBUG_GOLEM_TRIM,
                        "Village security trimmed " + removed
                                + " ordinary unnamed iron golem(s) at raid start; kept "
                                + Math.min(Math.max(0, keep), ordinary.size())
                                + " for difficulty=" + session.difficultyName + ".");
            }
        } catch (Throwable throwable) {
            if (!warnedGolemTrimFailure) {
                warnedGolemTrimFailure = true;
                System.out.println("[Raid Enhancement Patch] Ordinary iron golem raid-start trim failed once and was suppressed: " + throwable);
            }
        }
    }

    private static int ordinaryGolemKeepLimit(String difficultyName) {
        String key = difficultyKey(difficultyName);
        if (key.contains("HARD")) {
            return Math.max(0, RaidEnhancementConfig.VILLAGE_SECURITY_KEEP_ORDINARY_GOLEMS_HARD);
        }
        if (key.contains("NORMAL")) {
            return Math.max(0, RaidEnhancementConfig.VILLAGE_SECURITY_KEEP_ORDINARY_GOLEMS_NORMAL);
        }
        return Math.max(0, RaidEnhancementConfig.VILLAGE_SECURITY_KEEP_ORDINARY_GOLEMS_EASY);
    }

    private static List<Entity> ironGolemsNear(ServerLevel level, int x, int y, int z, int radius) {
        List<Entity> golems = new ArrayList<>();
        try {
            Class<?> ironGolemClass = Class.forName(IRON_GOLEM_CLASS_NAME);
            Object box = aabbAround(x, y, z, radius);
            Method getEntities = level.getClass().getMethod("getEntitiesOfClass", Class.class, box.getClass());
            Object result = getEntities.invoke(level, ironGolemClass, box);
            if (result instanceof List<?> list) {
                for (Object candidate : list) {
                    if (candidate instanceof Entity entity && entity.isAlive() && isIronGolem(entity)) {
                        golems.add(entity);
                    }
                }
            }
        } catch (Throwable ignored) {
            return List.of();
        }
        return golems;
    }

    private static List<Entity> trackedSecurityGolems(ServerLevel level, SecuritySession session, long gameTime) {
        List<Entity> alive = new ArrayList<>();
        Iterator<UUID> iterator = session.securityGolemIds.iterator();
        long pruneGrace = Math.max(200L, RaidEnhancementConfig.PERFORMANCE_SECURITY_GOLEM_MISSING_PRUNE_GRACE_TICKS);
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Entity entity = level.getEntity(uuid);
            if (entity == null) {
                session.missingSecurityGolemSinceGameTime.putIfAbsent(uuid, gameTime);
                Long missingSince = session.missingSecurityGolemSinceGameTime.get(uuid);
                if (missingSince != null && gameTime - missingSince >= pruneGrace) {
                    iterator.remove();
                    session.missingSecurityGolemSinceGameTime.remove(uuid);
                }
                continue;
            }
            if (!entity.isAlive() || !isIronGolem(entity)) {
                iterator.remove();
                session.missingSecurityGolemSinceGameTime.remove(uuid);
                continue;
            }
            session.missingSecurityGolemSinceGameTime.remove(uuid);
            session.lastObservedSecurityGolemGameTime = gameTime;
            alive.add(entity);
        }
        return alive;
    }

    private static void pruneTrackedGolems(ServerLevel level, SecuritySession session, long gameTime) {
        trackedSecurityGolems(level, session, gameTime);
    }

    private static Entity spawnIronGolem(ServerLevel level, int x, int y, int z, boolean securityGolem,
                                         SecuritySession session, int index) {
        try {
            Object entity = createEntityFromId(IRON_GOLEM_ID, level);
            if (!(entity instanceof Entity golem)) {
                return null;
            }
            moveEntity(entity, x + 0.5D, y, z + 0.5D);
            invokeOptionalNoArg(entity, "setPersistenceRequired");
            repairLivingToFull(entity);
            if (securityGolem) {
                applySecurityCombatEffects(entity);
            }
            boolean added = addFreshEntity(level, entity);
            if (!added) {
                return null;
            }
            if (securityGolem && session != null) {
                session.securityGolemIds.add(golem.getUUID());
                // Apply regular-army maintenance again after the entity has been inserted into the level.
                // Some 1.21.1 effect paths silently ignore pre-insertion MobEffectInstance changes.
                repairLivingToFull(entity);
                applySecurityCombatEffects(entity);
            }
            return golem;
        } catch (Throwable throwable) {
            if (!warnedSpawnFailure) {
                warnedSpawnFailure = true;
                System.out.println("[Raid Enhancement Patch] Village security iron golem spawn failed once: " + throwable);
            }
            return null;
        }
    }

    private static Object createEntityFromId(String entityId, ServerLevel level) throws ReflectiveOperationException {
        Class<?> entityTypeClass = Class.forName("net.minecraft.world.entity.EntityType");
        Method byString = entityTypeClass.getMethod("byString", String.class);
        Object optional = byString.invoke(null, entityId);
        Object entityType = null;
        if (optional instanceof Optional<?> opt && opt.isPresent()) {
            entityType = opt.get();
        }
        return entityType == null ? null : createEntity(entityType, level);
    }

    private static Object createEntity(Object entityType, ServerLevel level) throws ReflectiveOperationException {
        for (Method method : entityType.getClass().getMethods()) {
            if (!method.getName().equals("create") || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameterType = method.getParameterTypes()[0];
            if (!parameterType.isAssignableFrom(level.getClass())) {
                continue;
            }
            method.setAccessible(true);
            return method.invoke(entityType, level);
        }
        for (Method method : entityType.getClass().getDeclaredMethods()) {
            if (!method.getName().equals("create") || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameterType = method.getParameterTypes()[0];
            if (!parameterType.isAssignableFrom(level.getClass())) {
                continue;
            }
            method.setAccessible(true);
            return method.invoke(entityType, level);
        }
        return null;
    }

    private static void moveEntity(Object entity, double x, double y, double z) throws ReflectiveOperationException {
        try {
            Method moveTo = entity.getClass().getMethod("moveTo", double.class, double.class, double.class, float.class, float.class);
            moveTo.setAccessible(true);
            moveTo.invoke(entity, x, y, z, 0.0F, 0.0F);
            return;
        } catch (NoSuchMethodException ignored) {
            // Try setPos below.
        }
        Method setPos = entity.getClass().getMethod("setPos", double.class, double.class, double.class);
        setPos.setAccessible(true);
        setPos.invoke(entity, x, y, z);
    }

    private static boolean addFreshEntity(ServerLevel level, Object entity) throws ReflectiveOperationException {
        for (Method method : level.getClass().getMethods()) {
            if (!method.getName().equals("addFreshEntity") || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameterType = method.getParameterTypes()[0];
            if (!parameterType.isAssignableFrom(entity.getClass())) {
                continue;
            }
            method.setAccessible(true);
            Object result = method.invoke(level, entity);
            return !(result instanceof Boolean bool) || bool;
        }
        return false;
    }

    /**
     * 0.8.9.4: Player-damage shield for village iron golems during an active raid.
     *
     * This intentionally protects every iron golem inside the active village-security
     * radius, not only temporary security golems. Ordinary unnamed/named golems are
     * still normal entities again after the raid session is completed and removed.
     * Raider damage is not blocked here; callers must check that the damage source is
     * player-caused before cancelling incoming damage.
     */
    public static boolean shouldCancelPlayerDamageToVillageGolem(Entity entity) {
        if (!RaidEnhancementConfig.VILLAGE_SECURITY_ENABLED
                || RaidEnhancementConfig.VILLAGE_SECURITY_ALLOW_PLAYER_DAMAGE_TO_VILLAGE_GOLEMS_DURING_RAID
                || entity == null || !entity.isAlive() || !isIronGolem(entity)) {
            return false;
        }
        try {
            if (RaidEnhancementConfig.PERFORMANCE_ACTIVE_SESSION_FAST_CHECK_ENABLED && SESSIONS.isEmpty()) {
                return false;
            }
            long radiusSquared = (long) securityRadius() * (long) securityRadius();
            for (SecuritySession session : new ArrayList<>(SESSIONS.values())) {
                if (session == null) {
                    continue;
                }
                double dx = entity.getX() - (session.centerX + 0.5D);
                double dy = entity.getY() - session.centerY;
                double dz = entity.getZ() - (session.centerZ + 0.5D);
                if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            return false;
        }
        return false;
    }

    private static int cleanupSecurityGolems(ServerLevel level, SecuritySession session) {
        if (level == null || session == null) {
            return 0;
        }
        int cleaned = 0;
        for (UUID uuid : new ArrayList<>(session.securityGolemIds)) {
            Entity entity = level.getEntity(uuid);
            if (entity != null && isIronGolem(entity)) {
                discard(entity);
                cleaned++;
            }
        }
        session.securityGolemIds.clear();
        return cleaned;
    }

    private static void discard(Object entity) {
        try {
            Method discard = entity.getClass().getMethod("discard");
            discard.setAccessible(true);
            discard.invoke(entity);
        } catch (Throwable ignored) {
            try {
                Method remove = entity.getClass().getMethod("remove", Class.forName("net.minecraft.world.entity.Entity$RemovalReason"));
                Object reason = Enum.valueOf((Class<Enum>) remove.getParameterTypes()[0].asSubclass(Enum.class), "DISCARDED");
                remove.invoke(entity, reason);
            } catch (Throwable ignoredAgain) {
                // Fail closed; do not kill ordinary golems by mistake.
            }
        }
    }

    private static boolean isIronGolem(Object entity) {
        try {
            return Class.forName(IRON_GOLEM_CLASS_NAME).isInstance(entity);
        } catch (Throwable ignored) {
            return entity != null && entity.getClass().getName().endsWith("IronGolem");
        }
    }

    private static boolean hasCustomName(Entity entity) {
        if (entity == null) {
            return false;
        }
        try {
            Method method = entity.getClass().getMethod("hasCustomName");
            Object result = method.invoke(entity);
            if (result instanceof Boolean bool) {
                return bool;
            }
        } catch (Throwable ignored) {
            // Try getCustomName below.
        }
        try {
            Method method = entity.getClass().getMethod("getCustomName");
            return method.invoke(entity) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static double distanceSquared(Entity entity, int x, int y, int z) {
        if (entity == null) {
            return Double.MAX_VALUE;
        }
        double dx = entity.getX() - (x + 0.5D);
        double dy = entity.getY() - y;
        double dz = entity.getZ() - (z + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private static void applySecurityGlow(Object livingEntity) {
        MobEffectCompat.addEffect(livingEntity, MobEffectCompat.GLOWING_NAMES,
                Math.max(20, RaidEnhancementConfig.VILLAGE_SECURITY_GOLEM_GLOW_DURATION_TICKS), 0);
    }

    private static void applySecurityCombatEffects(Object livingEntity) {
        applySecurityGlow(livingEntity);
        if (RaidEnhancementConfig.VILLAGE_SECURITY_GOLEM_RESISTANCE_ENABLED) {
            int level = Math.max(1, RaidEnhancementConfig.VILLAGE_SECURITY_GOLEM_RESISTANCE_LEVEL);
            MobEffectCompat.addVisibleEffect(livingEntity, MobEffectCompat.RESISTANCE_NAMES,
                    Math.max(20, RaidEnhancementConfig.VILLAGE_SECURITY_GOLEM_RESISTANCE_DURATION_TICKS), level - 1);
        }
    }

    private static void repairSecuritySurvivor(Object livingEntity) {
        if (livingEntity == null) {
            return;
        }
        if (RaidEnhancementConfig.VILLAGE_SECURITY_SURVIVOR_REPAIR_TO_FULL) {
            repairLivingToFull(livingEntity);
            return;
        }
        float current = getFloatNoArg(livingEntity, "getHealth", 1.0F);
        float max = getFloatNoArg(livingEntity, "getMaxHealth", 20.0F);
        float amount = Math.max(0.0F, RaidEnhancementConfig.VILLAGE_SECURITY_SURVIVOR_REPAIR_HEALTH);
        if (amount > 0.0F) {
            setHealth(livingEntity, Math.max(1.0F, Math.min(max, current + amount)));
        }
    }

    private static int redeployExistingSecurityGolems(ServerLevel level, SecuritySession session, List<Entity> aliveGolems,
                                                      int wave, int target) {
        if (!RaidEnhancementConfig.VILLAGE_SECURITY_REDEPLOY_EXISTING_GOLEMS_ON_WAVE_START
                || level == null || session == null || aliveGolems == null || aliveGolems.isEmpty()) {
            return 0;
        }
        int redeployed = 0;
        int pointCount = Math.max(1, Math.max(RaidEnhancementConfig.VILLAGE_SECURITY_GOLEM_SPAWN_POINT_COUNT,
                Math.max(target, aliveGolems.size())));
        int radius = Math.max(1, RaidEnhancementConfig.VILLAGE_SECURITY_GOLEM_REDEPLOY_RING_RADIUS);
        for (int i = 0; i < aliveGolems.size(); i++) {
            Entity golem = aliveGolems.get(i);
            if (golem == null || !golem.isAlive()) {
                continue;
            }
            int[] pos = deploymentPosition(level, session, i, pointCount, radius, wave, false);
            try {
                moveEntity(golem, pos[0] + 0.5D, pos[1], pos[2] + 0.5D);
                redeployed++;
            } catch (Throwable ignored) {
                // Re-deployment is tactical polish. If movement fails, leave the golem where it is.
            }
        }
        return redeployed;
    }

    private static void repairLivingToFull(Object livingEntity) {
        try {
            float max = getFloatNoArg(livingEntity, "getMaxHealth", 20.0F);
            setHealth(livingEntity, Math.max(1.0F, max));
        } catch (Throwable ignored) {
            // Optional repair only.
        }
    }

    private static float getFloatNoArg(Object target, String methodName, float fallback) {
        if (target == null) {
            return fallback;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            if (result instanceof Number number) {
                return number.floatValue();
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return fallback;
    }

    private static boolean setHealth(Object livingEntity, float health) {
        if (livingEntity == null) {
            return false;
        }
        for (Method method : livingEntity.getClass().getMethods()) {
            if (!method.getName().equals("setHealth") || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> type = method.getParameterTypes()[0];
            try {
                method.setAccessible(true);
                if (type == float.class || type == Float.TYPE) {
                    method.invoke(livingEntity, health);
                    return true;
                }
                if (type == double.class || type == Double.TYPE) {
                    method.invoke(livingEntity, (double) health);
                    return true;
                }
            } catch (Throwable ignored) {
                return false;
            }
        }
        return false;
    }

    private static int[] ringPosition(ServerLevel level, int centerX, int centerY, int centerZ, int index, int count, int radius) {
        double angle = (Math.PI * 2.0D * Math.max(0, index)) / Math.max(1, count);
        int x = centerX + (int) Math.round(Math.cos(angle) * Math.max(1, radius));
        int z = centerZ + (int) Math.round(Math.sin(angle) * Math.max(1, radius));
        int y = Math.max(centerY, motionBlockingHeight(level, x, z) + 1);
        return new int[]{x, y, z};
    }

    private static int[] deploymentPosition(ServerLevel level, SecuritySession session, int index, int count,
                                            int radius, int wave, boolean spawn) {
        int safeCount = Math.max(1, count);
        int safeRadius = Math.max(1, radius);
        double waveOffset = ((Math.max(1, wave) - 1) % safeCount) * (Math.PI * 2.0D / safeCount) * 0.5D;
        if (spawn) {
            waveOffset += Math.PI / Math.max(4.0D, safeCount);
        }
        double angle = (Math.PI * 2.0D * Math.max(0, index)) / safeCount + waveOffset;
        int x = session.centerX + (int) Math.round(Math.cos(angle) * safeRadius);
        int z = session.centerZ + (int) Math.round(Math.sin(angle) * safeRadius);
        int y = Math.max(session.centerY, motionBlockingHeight(level, x, z) + 1);
        return new int[]{x, y, z};
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int motionBlockingHeight(ServerLevel level, int x, int z) {
        try {
            Class<?> heightmapTypesClass = Class.forName("net.minecraft.world.level.levelgen.Heightmap$Types");
            Object motionBlocking = Enum.valueOf((Class<Enum>) heightmapTypesClass.asSubclass(Enum.class), "MOTION_BLOCKING");
            for (Method method : level.getClass().getMethods()) {
                if (!method.getName().equals("getHeight") || method.getParameterCount() != 3) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                if (!types[0].isAssignableFrom(motionBlocking.getClass())) {
                    continue;
                }
                Object result = method.invoke(level, motionBlocking, x, z);
                if (result instanceof Number number) {
                    return number.intValue();
                }
            }
        } catch (Throwable ignored) {
            // Fall back to raid center Y below.
        }
        return 64;
    }

    private static Object aabbAround(int x, int y, int z, int radius) throws ReflectiveOperationException {
        Class<?> aabbClass = Class.forName(AABB_CLASS_NAME);
        Constructor<?> constructor = aabbClass.getConstructor(double.class, double.class, double.class,
                double.class, double.class, double.class);
        return constructor.newInstance(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
    }

    private static int securityRadius() {
        return Math.max(16, Math.max(RaidEnhancementConfig.VILLAGE_SECURITY_RADIUS,
                Math.max(RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS, RaidEnhancementConfig.AUTO_RAID_RAIDER_FALLBACK_RADIUS)));
    }

    private static int penaltyRadius() {
        return Math.max(securityRadius(), Math.max(16, RaidEnhancementConfig.VILLAGE_SECURITY_VILLAGER_PENALTY_RADIUS));
    }

    private static void invokeOptionalNoArg(Object target, String methodName) {
        if (target == null || methodName == null) {
            return;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Optional compatibility call.
        }
    }


    private static List<Villager> refreshTrackedVillagers(ServerLevel level, SecuritySession session, long gameTime) {
        if (level == null || session == null) {
            return List.of();
        }
        List<Villager> villagers = aliveVillagers(level, session.centerX, session.centerY, session.centerZ, securityRadius());
        session.villagerIds.clear();
        for (Villager villager : villagers) {
            session.villagerIds.add(villager.getUUID());
        }
        session.lastVillagerRefreshGameTime = gameTime;
        return villagers;
    }

    private static List<Villager> trackedVillagers(ServerLevel level, SecuritySession session) {
        if (level == null || session == null || session.villagerIds.isEmpty()) {
            return List.of();
        }
        List<Villager> villagers = new ArrayList<>();
        Iterator<UUID> iterator = session.villagerIds.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Entity entity = level.getEntity(uuid);
            if (!(entity instanceof Villager villager) || !villager.isAlive()) {
                iterator.remove();
                continue;
            }
            villagers.add(villager);
        }
        return villagers;
    }

    private static void announceWavePreparation(ServerLevel level, SecuritySession session, int wave,
                                                int repaired, int spawned, int redeployed, int active, int target) {
        if (!RaidEnhancementConfig.VILLAGE_SECURITY_MESSAGES_ENABLED || level == null || session == null) {
            return;
        }
        if (wave <= 1 && !session.initialDeploymentAnnounced) {
            session.initialDeploymentAnnounced = true;
            if (RaidEnhancementConfig.VILLAGE_SECURITY_INITIAL_MESSAGES_ENABLED) {
                sendSecurityMessage(level, session.centerX, session.centerY, session.centerZ, pick(INITIAL_ENTRY_MESSAGES));
                sendSecurityMessage(level, session.centerX, session.centerY, session.centerZ,
                        formatPick(INITIAL_DEPLOYMENT_MESSAGES, Math.max(0, active)));
            }
            maybeAnnounceCombatBuff(level, session);
            return;
        }
        if (wave <= 1) {
            maybeAnnounceCombatBuff(level, session);
            return;
        }
        if (redeployed > 0 && RaidEnhancementConfig.VILLAGE_SECURITY_REDEPLOYMENT_MESSAGES_ENABLED) {
            sendSecurityMessage(level, session.centerX, session.centerY, session.centerZ,
                    formatPick(REDEPLOYMENT_MESSAGES, wave, redeployed));
        }
        if (spawned > 0 && RaidEnhancementConfig.VILLAGE_SECURITY_REINFORCEMENT_MESSAGES_ENABLED) {
            sendSecurityMessage(level, session.centerX, session.centerY, session.centerZ,
                    formatPick(REINFORCEMENT_MESSAGES, wave, spawned, active, target));
        } else if (repaired > 0 && RaidEnhancementConfig.VILLAGE_SECURITY_REPAIR_MESSAGES_ENABLED) {
            sendSecurityMessage(level, session.centerX, session.centerY, session.centerZ,
                    formatPick(REPAIR_MESSAGES, wave, repaired, active, target));
            if (RaidEnhancementConfig.VILLAGE_SECURITY_COMBAT_BUFF_MESSAGES_ENABLED) {
                sendSecurityMessage(level, session.centerX, session.centerY, session.centerZ,
                        formatPick(SURVIVOR_REPAIR_MESSAGES, repaired));
            }
        }
        maybeAnnounceCombatBuff(level, session);
    }

    private static void maybeAnnounceCombatBuff(ServerLevel level, SecuritySession session) {
        if (level == null || session == null || session.combatBuffAnnounced) {
            return;
        }
        if (!RaidEnhancementConfig.VILLAGE_SECURITY_GOLEM_RESISTANCE_ENABLED
                || !RaidEnhancementConfig.VILLAGE_SECURITY_COMBAT_BUFF_MESSAGES_ENABLED) {
            return;
        }
        session.combatBuffAnnounced = true;
        sendSecurityMessage(level, session.centerX, session.centerY, session.centerZ,
                formatPick(COMBAT_BUFF_MESSAGES, Math.max(1, RaidEnhancementConfig.VILLAGE_SECURITY_GOLEM_RESISTANCE_LEVEL)));
    }

    private static void sendSecurityMessage(ServerLevel level, int centerX, int centerY, int centerZ, String message) {
        if (!RaidEnhancementConfig.VILLAGE_SECURITY_MESSAGES_ENABLED || level == null || message == null || message.isBlank()) {
            return;
        }
        try {
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Method literal = componentClass.getMethod("literal", String.class);
            Object component = literal.invoke(null, message);
            int radius = Math.max(64, securityRadius());
            long radiusSquared = (long) radius * (long) radius;
            for (Object playerObject : playersSnapshot(level)) {
                if (!(playerObject instanceof Entity player) || !player.isAlive()) {
                    continue;
                }
                double dx = player.getX() - (centerX + 0.5D);
                double dy = player.getY() - centerY;
                double dz = player.getZ() - (centerZ + 0.5D);
                if (dx * dx + dy * dy + dz * dz > radiusSquared) {
                    continue;
                }
                for (Method method : player.getClass().getMethods()) {
                    if (!method.getName().equals("sendSystemMessage") || method.getParameterCount() != 1) {
                        continue;
                    }
                    if (!method.getParameterTypes()[0].isAssignableFrom(componentClass)) {
                        continue;
                    }
                    method.setAccessible(true);
                    method.invoke(player, component);
                    break;
                }
            }
        } catch (Throwable ignored) {
            // Chat feedback is optional and must never affect raid logic.
        }
    }

    private static List<?> playersSnapshot(ServerLevel level) {
        try {
            Method playersMethod = level.getClass().getMethod("players");
            Object result = playersMethod.invoke(level);
            if (result instanceof List<?> list) {
                return List.copyOf(list);
            }
        } catch (ReflectiveOperationException ignored) {
            // Fail closed.
        }
        return List.of();
    }

    private static String pick(String[] messages) {
        if (messages == null || messages.length == 0) {
            return "";
        }
        int index = java.util.concurrent.ThreadLocalRandom.current().nextInt(messages.length);
        return messages[index];
    }

    private static String formatPick(String[] messages, Object... args) {
        String template = pick(messages);
        try {
            return String.format(java.util.Locale.ROOT, template, args);
        } catch (Throwable ignored) {
            return template;
        }
    }

    private static String damageText(float damage) {
        if (Math.abs(damage - Math.round(damage)) < 0.001F) {
            return Integer.toString(Math.round(damage));
        }
        return Float.toString(damage);
    }

    private static void debugLog(boolean detailSwitchEnabled, String message) {
        if (!RaidEnhancementConfig.VILLAGE_SECURITY_DEBUG_LOGS_ENABLED || !detailSwitchEnabled
                || message == null || message.isBlank()) {
            return;
        }
        System.out.println("[Raid Enhancement Patch] " + message);
    }

    private static String difficultyKey(String difficultyName) {
        return difficultyName == null ? "" : difficultyName.toUpperCase(java.util.Locale.ROOT);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class SecuritySession {
        final String raidKey;
        String dimensionId;
        String difficultyName;
        int centerX;
        int centerY;
        int centerZ;
        int currentWave = -1;
        long lastSeenGameTime;
        long lastTickGameTime;
        long lastVillagerRefreshGameTime;
        long lastGolemGlowGameTime;
        long lastNoActiveCombatGameTime;
        long lastObservedSecurityGolemGameTime;
        long allSecurityGolemsMissingSinceGameTime;
        boolean waveDamageApplied;
        boolean currentWaveTimeoutPenaltyApplied;
        long lastTimeoutPenaltyCheckGameTime;
        boolean hadSecurityGolemsThisWave;
        int lastWaveStartMaintenanceWave = -1;
        boolean ordinaryGolemTrimmed;
        boolean initialDeploymentAnnounced;
        int wavesPrepared;
        int highestPreparedWave;
        int defenseFailureCount;
        int totalBreachDamageEvents;
        int totalBreachDamagedVillagers;
        int timeoutPenaltyCount;
        int totalTimeoutDamagedVillagers;
        int totalSpawnedSecurityGolems;
        int totalRepairedSecurityGolems;
        int totalRedeployedSecurityGolems;
        int nextSpawnPointCursor;
        boolean combatBuffAnnounced;
        final List<UUID> securityGolemIds = new ArrayList<>();
        final List<UUID> villagerIds = new ArrayList<>();
        final Map<UUID, Long> missingSecurityGolemSinceGameTime = new LinkedHashMap<>();

        SecuritySession(String raidKey, String dimensionId, String difficultyName) {
            this.raidKey = raidKey;
            this.dimensionId = dimensionId;
            this.difficultyName = difficultyName;
        }
    }
}
