package com.bank.dualrecord.model;

/**
 * 订单状态机
 *
 * 状态流转:
 * <pre>
 *   RESERVED → VERIFIED → SCRIPTING → RECORDING → SIGNING → QA_PASSED → COMPLETED
 *      ↓         ↓           ↓             ↓           ↓          ↓
 *   CANCELLED  FAILED     FAILED       FAILED     FAILED    FAILED
 * </pre>
 *
 * 注:COMPLETED 为终态,不可再流转
 *
 * @author Mavis
 */
public enum OrderState {

    RESERVED(0, "已预约"),
    VERIFIED(1, "已核验"),
    SCRIPTING(2, "话术执行中"),
    RECORDING(3, "视频录制中"),
    SIGNING(4, "电子签约"),
    QA_PASSED(5, "质检通过"),
    COMPLETED(6, "订单完成"),
    CANCELLED(-1, "已取消"),
    FAILED(-2, "已失败");

    private final int code;
    private final String desc;

    OrderState(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static OrderState fromCode(int code) {
        for (OrderState s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown order state: " + code);
    }

    public static OrderState fromName(String name) {
        try {
            return OrderState.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown order state: " + name);
        }
    }

    public boolean isTerminal() {
        return this == COMPLETED;
    }

    public boolean isAbnormal() {
        return this == CANCELLED || this == FAILED;
    }
}
