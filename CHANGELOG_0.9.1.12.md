# Changelog 0.9.1.12-raid-registration-lifecycle-hotfix

- Fix patch-owned ground raiders being offered to the server world twice.
- Keep `ServerLevel#addFreshEntity` as the sole insertion commit and use vanilla
  `Raid#joinRaid(..., true)` only for native wave membership registration.
- Force-checkpoint active bounded spawn queues during `ServerStoppingEvent`.
- Clear process-local extra-wave, HUD snapshot, lifecycle mirror, raid session
  and encounter read-model state during `ServerStoppedEvent`.
- Preserve the lifecycle sidecar so the next server instance must validate and
  restore pending work from disk.
- Add static contracts that reject a reintroduced `joinRaid(..., false)` call or
  missing server lifecycle listener.
- Keep `SafeRaidSpawnResolver`, wave composition, configuration defaults and
  enabled Mixins unchanged.
