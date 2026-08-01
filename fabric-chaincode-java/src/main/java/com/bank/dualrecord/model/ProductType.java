package com.bank.dualrecord.model;

/**
 * 产品类型
 */
public enum ProductType {
    INSURANCE(1, "保险"),
    WEALTH(2, "理财"),
    FUND(3, "基金"),
    TRUST(4, "信托"),
    GOLD(5, "贵金属");

    private final int code;
    private final String desc;

    ProductType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static ProductType fromCode(int code) {
        for (ProductType p : values()) {
            if (p.code == code) return p;
        }
        throw new IllegalArgumentException("Unknown product type: " + code);
    }
}
