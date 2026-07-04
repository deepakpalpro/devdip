package com.banking.forms.shared;

import java.util.UUID;

public final class Uuids {

    private Uuids() {}

    public static byte[] toBytes(UUID uuid) {
        var buffer = java.nio.ByteBuffer.wrap(new byte[16]);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    public static UUID fromBytes(byte[] bytes) {
        var buffer = java.nio.ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
