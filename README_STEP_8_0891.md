# raid_enhancement_patch 0.8.9.1-village-security-configurable

Baseline: 0.8.9.0-village-security-messages-performance.

This build adds a lightweight runtime configuration layer for the village-security system only. It intentionally does not modify the HUD, native raid failure state, extra-wave bridge, 9-11 wave state machine, leave/return freeze handling, Raids Enhanced special spawning, raider clearing, or raider teleporting.

## Runtime config file

On first launch, the mod creates:

`config/raid_enhancement_patch/village_security.properties`

A reference copy is also written as:

`config/raid_enhancement_patch/village_security.default.properties`

Restart the game/server after editing.

## Configurable groups

- Enable/disable village security.
- Enable/disable all Village Defense League chat reports.
- Enable/disable individual message groups: initial entry, reinforcement, repair, per-wave defense failure, victory, and failure.
- Security tick interval.
- Villager cache refresh interval.
- Security golem glowing refresh interval.
- Village security radius.
- Villagers-per-security-golem by difficulty.
- Minimum and maximum security golems per wave.
- Per-wave breach damage to villagers.
- Security golem glowing duration and spawn-ring radius.
- Victory ordinary golem restoration count by difficulty.
- Strict rules: villager anti-heal clamp, clamp duration, ordinary unnamed golem trimming, and ordinary unnamed golem keep limits by difficulty.

## Preserved behavior

- Security golems remain temporary managed battle units from the Village Defense League / 村民防卫同盟.
- Ordinary unnamed golems remain the narrative “ordinary mercenary” daily defense force.
- Per-wave security golem wipe still applies one breach-damage event.
- Villager allowed-health clamp still prevents outside healing from erasing the village-security pressure.
- Victory still restores living villagers and returns ordinary golems according to difficulty.
- Non-victory ending still releases health clamps without victory healing.
