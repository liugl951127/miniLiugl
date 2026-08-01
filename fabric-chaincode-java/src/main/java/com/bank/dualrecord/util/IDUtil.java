package com.bank.dualrecord.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * ID 生成器
 */
public final class IDUtil {

    private static final AtomicLong SEQ = new AtomicLong(1);
    private static final DateTimeFormatter ORDER_FMT =
        DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("Asia/Shanghai"));

    /** 订单号格式:ORD + yyyyMMdd + 6 位流水 */
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("^ORD\\d{14}$");
    /** 会话号格式:SES + yyyyMMddHHmmss + 3 位 */
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("^SES\\d{17}$");

    private IDUtil() {
    }

    /**
     * 生成订单号:ORD + yyyyMMdd + 6 位流水
     * 例:ORD20260801000001
     */
    public static String generateOrderId() {
        String date = ORDER_FMT.format(Instant.now());
        long seq = SEQ.getAndIncrement() % 1_000_000;
        return String.format("ORD%s%06d", date, seq);
    }

    /**
     * 生成会话号:SES + yyyyMMddHHmmss + 3 位
     */
    public static String generateSessionId() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.of("Asia/Shanghai"));
        String time = fmt.format(Instant.now());
        long seq = SEQ.getAndIncrement() % 1000;
        return String.format("SES%s%03d", time, seq);
    }

    /**
     * 生成 UUID(去掉横线,32 字符)
     */
    public static String generateUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 验证订单号
     */
    public static boolean isValidOrderId(String orderId) {
        return orderId != null && ORDER_ID_PATTERN.matcher(orderId).matches();
    }

    /**
     * 验证会话号
     */
    public static boolean isValidSessionId(String sessionId) {
        return sessionId != null && SESSION_ID_PATTERN.matcher(sessionId).matches();
    }

    /**
     * 生成证据 ID:EV + UUID 前 16 位
     */
    public static String generateEvidenceId() {
        return "EV" + generateUuid().substring(0, 16);
    }

    /**
     * 生成合同 ID:CT + UUID 前 16 位
     */
    public static String generateContractId() {
        return "CT" + generateUuid().substring(0, 16);
    }
}
