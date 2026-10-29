package com.gjsk.mediagateway.common.util;

import java.security.SecureRandom;

/**
 * ID 生成工具。
 *
 * <p>业务主键统一使用 UUIDv7 十六进制字符串（32 位小写，无连字符），
 * 时间有序、索引友好，便于对外暴露与分库分表。</p>
 */
public final class IdUtils {

    private static final int TOKEN_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private IdUtils() {
    }

    /**
     * 生成 UUIDv7 字符串（32 位小写十六进制）。
     *
     * <p>结构：48 位毫秒时间戳 + 4 位版本(7) + 12 位随机 + 2 位变体(10) + 62 位随机。</p>
     */
    public static String newId() {
        long timestamp = System.currentTimeMillis() & 0xFFFF_FFFF_FFFFL;
        long randA = RANDOM.nextInt(0x1000);
        long msb = (timestamp << 16) | (0x7L << 12) | randA;
        long randB = RANDOM.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL;
        long lsb = (0x2L << 62) | randB;
        return toHex(msb, lsb);
    }

    /**
     * 生成会话令牌：128 位安全随机数，十六进制字符串。
     */
    public static String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            chars[i * 2] = HEX[value >>> 4];
            chars[i * 2 + 1] = HEX[value & 0x0F];
        }
        return new String(chars);
    }

    /**
     * 生成请求追踪 ID（沿用 UUIDv7）。
     */
    public static String newRequestId() {
        return newId();
    }

    private static String toHex(long msb, long lsb) {
        char[] chars = new char[32];
        for (int i = 0; i < 16; i++) {
            int shift = (15 - i) * 4;
            int value = (int) ((msb >>> shift) & 0x0F);
            chars[i] = HEX[value];
        }
        for (int i = 0; i < 16; i++) {
            int shift = (15 - i) * 4;
            int value = (int) ((lsb >>> shift) & 0x0F);
            chars[16 + i] = HEX[value];
        }
        return new String(chars);
    }
}
