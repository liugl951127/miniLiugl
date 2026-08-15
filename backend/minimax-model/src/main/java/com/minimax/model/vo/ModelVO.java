package com.minimax.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ModelVO {
    private Long id;
    private String code;
    private String displayName;
    private Integer maxContext;
    private Integer maxOutput;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private Boolean supportsVision;
    private Boolean supportsTools;
    private Boolean supportsStream;

    private Long providerId;
    private String providerCode;
    private String providerName;
    private String protocol;
}
