# BUILD_VERIFICATION_0.9.1.0

Version:

`0.9.1.0-victory-bar-attach-guard-alpha`

## Build status

Sandbox Gradle clean build was attempted but could not complete because the wrapper tried to download Gradle 8.12 from `services.gradle.org`, which is unavailable in the sandbox environment.

Therefore, this handoff includes:

- a GitHub-ready source package with version metadata updated to 0.9.1.0;
- a locally repacked JAR generated from the user-provided 0.9.0.9 Gradle-built JAR by applying same-length version-string updates to classes and resources;
- SHA-256 checksums for both artifacts.

## Verification performed

Static checks performed on the generated JAR:

- JAR structure readable.
- `META-INF/MANIFEST.MF` contains `Implementation-Version: 0.9.1.0-victory-bar-attach-guard-alpha`.
- `META-INF/neoforge.mods.toml` contains `version="0.9.1.0-victory-bar-attach-guard-alpha"`.
- `RaidEnhancementPatch.class` contains `0.9.1.0-victory-bar-attach-guard-alpha`.
- `RaidIndependentBossbarManager$VictoryAttachGuard.class` exists.
- `victory-bar-attach-blocked` exists.
- `victory-attach-guard-summary` exists.
- `victory-attach-guard-skipped-different-dimension` exists.
- `skipped-different-dimension` exists.
- `raid_enhancement_patch.mixins.json` does not enable `ServerBossEventRaidTitleMixin`.

## Required formal verification

Recommended next step:

Run GitHub Actions or local Gradle clean build from the GitHub-ready source package, then compare behavior with this generated JAR.

Recommended game test:

- Delete `config/raid_enhancement_patch/key_diagnostics.log` before the test.
- Confirm `[REP]` BossBar visible.
- Confirm first wave decreases.
- Confirm second wave refills once.
- Confirm second wave decreases.
- Confirm completion removes `[REP]`.
- Confirm no no-`[REP]` vanilla victory bar remains visible.
- Confirm `VictoryBarAttachGuard` summary appears.
- Confirm `settlementKeyMode=raidInstance`.
- Confirm no `duplicate-blocked-by-history` and no `@raidInstance:fallback`.
