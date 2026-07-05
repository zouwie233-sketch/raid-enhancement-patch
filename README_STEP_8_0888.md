# raid_enhancement_patch 0.8.8.8-village-security-preview

Baseline: user-tested stable `0.8.8.7-native-bridge-return-freeze`.

This is phase 1 of the village-security redesign. It does not replace vanilla raid failure and does not clear, teleport, or forcibly fail raiders.

Implemented:

- Temporary village-security iron golems are maintained per raid session.
- At the start of each logical wave, security golems are repaired and refilled.
- Target security golem count is based on living villagers near the raid center:
  - Easy: 1 per 5 villagers
  - Normal: 1 per 8 villagers
  - Hard: 1 per 10 villagers
  - Always at least 1 and at most 5
- Security golems receive glowing so players can identify the true security force.
- If all managed security golems for the current wave die, each living villager near the raid center receives 4 direct health damage once for that wave.
- On successful completion, managed security golems are recovered, surviving villagers are healed to full, and ordinary daily-defense iron golems are spawned:
  - Easy: 1
  - Normal: 2
  - Hard: 3
- Previous villager protection no longer applies regeneration/absorption while village security is enabled, so breach damage is not immediately erased by the old protection layer.

Not implemented in this first phase:

- Clearing pre-existing ordinary village iron golems.
- Strict blocking of all external healing sources.
- Hard replacement of vanilla raid failure.
- Raider teleporting, clearing, or direct vanilla Raid loss mutation.

Build note: this JAR was compiled in the staged environment with local compile-time stubs and packaged without any Minecraft/NeoForge/RaidsEnhanced/fdlib classes.
