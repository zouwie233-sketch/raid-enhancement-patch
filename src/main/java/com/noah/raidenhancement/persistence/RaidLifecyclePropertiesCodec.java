package com.noah.raidenhancement.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

/** Bounded JDK-only codec for the existing raid lifecycle properties schema. */
public final class RaidLifecyclePropertiesCodec {
    public static final int MAX_PAYLOAD_BYTES = 4 * 1024 * 1024;

    private RaidLifecyclePropertiesCodec() {
    }

    public static byte[] encode(Properties properties) {
        Properties safeProperties = new Properties();
        if (properties != null) {
            safeProperties.putAll(properties);
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            safeProperties.store(output, "Raid Enhancement Patch save-scoped raid lifecycle metadata");
            byte[] payload = output.toByteArray();
            requireBounded(payload.length);
            return payload;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode raid lifecycle metadata", exception);
        }
    }

    public static Properties decode(byte[] payload) {
        Properties properties = new Properties();
        if (payload == null || payload.length == 0) {
            return properties;
        }
        requireBounded(payload.length);
        try (ByteArrayInputStream input = new ByteArrayInputStream(payload)) {
            properties.load(input);
            return properties;
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Failed to decode raid lifecycle metadata", exception);
        }
    }

    private static void requireBounded(int payloadLength) {
        if (payloadLength < 0 || payloadLength > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Raid lifecycle metadata exceeds the 4 MiB safety limit: "
                    + payloadLength + " bytes");
        }
    }
}
