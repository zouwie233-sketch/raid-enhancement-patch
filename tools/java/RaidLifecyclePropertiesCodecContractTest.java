package com.noah.raidenhancement.persistence;

import java.util.Properties;

/** JDK-only contract coverage for the bounded SavedData payload codec. */
public final class RaidLifecyclePropertiesCodecContractTest {
    private RaidLifecyclePropertiesCodecContractTest() {
    }

    public static void main(String[] args) {
        roundTripsLifecycleFields();
        isolatesReturnedProperties();
        rejectsOversizedPayloads();
        System.out.println("RaidLifecyclePropertiesCodecContractTest: PASS");
    }

    private static void roundTripsLifecycleFields() {
        Properties source = new Properties();
        source.setProperty("dataVersion", "1");
        source.setProperty("keys", "encoded-a,encoded-b");
        source.setProperty("session.encoded-a.key", "minecraft:overworld@center:12,64,-8");
        source.setProperty("session.encoded-a.spawnQueue.pendingCount", "3");

        Properties decoded = RaidLifecyclePropertiesCodec.decode(
                RaidLifecyclePropertiesCodec.encode(source));
        check(source.equals(decoded), "round trip must preserve every lifecycle property");
    }

    private static void isolatesReturnedProperties() {
        Properties source = new Properties();
        source.setProperty("keys", "one");
        byte[] payload = RaidLifecyclePropertiesCodec.encode(source);
        Properties first = RaidLifecyclePropertiesCodec.decode(payload);
        first.setProperty("keys", "changed");
        Properties second = RaidLifecyclePropertiesCodec.decode(payload);
        check("one".equals(second.getProperty("keys")), "decoded mutations must not change stored payload");
    }

    private static void rejectsOversizedPayloads() {
        byte[] oversized = new byte[RaidLifecyclePropertiesCodec.MAX_PAYLOAD_BYTES + 1];
        boolean rejected = false;
        try {
            RaidLifecyclePropertiesCodec.decode(oversized);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, "payloads above the safety bound must be rejected");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
