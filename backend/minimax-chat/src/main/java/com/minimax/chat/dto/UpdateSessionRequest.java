package com.minimax.chat.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateSessionRequest {

    @Size(max = 255)
    private String title;

    @Size(max = 64)
    private String model;

    @Size(max = 4000)
    private String systemPrompt;

    private BigDecimal temperature;

    /** 0 归档 / 1 正常 */
    private Integer status;
}
