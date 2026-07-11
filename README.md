# Raid Enhancement Patch

Current version: `0.9.1.6-config-audit-alpha`

0.9.1.6 is a read-only configuration audit version. It preserves the tested 0.9.1.5 VillageFavorGateway boundary and existing gameplay behavior, adds a startup audit marker, and ships a 417-row configuration inventory plus a focused Markdown summary.

It does not delete, rename, migrate or change any configuration key, default, loaded value or runtime consumer. It also does not change VillageFavor, rewards, gifts, cooldowns, persistence, keys, BossBar behavior, raid waves or Mixin enablement.

## Current architecture-governance delivery

`0.9.1.6-config-audit-alpha` identifies active, hardcoded and loaded-but-unused configuration entries. `rareGiftChanceMultiplier` and `equalXpPerEligiblePlayer` are documented as currently ineffective, but are not fixed in this version.
