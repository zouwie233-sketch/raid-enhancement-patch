package com.noah.raidenhancement.persistence;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Arrays;

/** Per-save Minecraft storage envelope for raid lifecycle and bounded spawn-queue metadata. */
final class RaidLifecycleSavedData extends SavedData {
    static final int DATA_VERSION = 1;
    static final Factory<RaidLifecycleSavedData> FACTORY =
            new Factory<>(RaidLifecycleSavedData::create, RaidLifecycleSavedData::load);

    private static final String DATA_VERSION_TAG = "dataVersion";
    private static final String PAYLOAD_TAG = "propertiesPayload";

    private byte[] payload = new byte[0];

    private static RaidLifecycleSavedData create() {
        return new RaidLifecycleSavedData();
    }

    private static RaidLifecycleSavedData load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        RaidLifecycleSavedData data = create();
        if (tag != null) {
            int storedVersion = tag.getInt(DATA_VERSION_TAG);
            if (storedVersion > DATA_VERSION) {
                throw new IllegalStateException("Unsupported future raid lifecycle data version: " + storedVersion);
            }
            data.payload = boundedCopy(tag.getByteArray(PAYLOAD_TAG));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(DATA_VERSION_TAG, DATA_VERSION);
        tag.putByteArray(PAYLOAD_TAG, payload);
        return tag;
    }

    synchronized byte[] payloadCopy() {
        return Arrays.copyOf(payload, payload.length);
    }

    synchronized void replacePayload(byte[] replacement) {
        byte[] safeReplacement = boundedCopy(replacement);
        if (Arrays.equals(payload, safeReplacement)) {
            return;
        }
        payload = safeReplacement;
        setDirty();
    }

    private static byte[] boundedCopy(byte[] source) {
        byte[] safeSource = source == null ? new byte[0] : source;
        if (safeSource.length > RaidLifecyclePropertiesCodec.MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Saved raid lifecycle metadata exceeds the 4 MiB safety limit");
        }
        return Arrays.copyOf(safeSource, safeSource.length);
    }
}
