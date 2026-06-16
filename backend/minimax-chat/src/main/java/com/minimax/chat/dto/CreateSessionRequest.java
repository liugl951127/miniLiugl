package com.minimax.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateSessionRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 64)
    private String model;

    @Size(max = 4000)
    private String systemPrompt;

    /** 0 ~ 2，默认 0.7 */
    private BigDecimal temperature = new BigDecimal("0.70");
}
