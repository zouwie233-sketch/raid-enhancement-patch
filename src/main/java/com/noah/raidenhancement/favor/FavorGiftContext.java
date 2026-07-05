package com.noah.raidenhancement.favor;

/** Immutable calculation result for one profession gift attempt. */
public record FavorGiftContext(String professionGroup, int villagerLevel, int giftTier, String giftTierName,
                               int emeraldBonusCap, boolean masterVillager) {
}
