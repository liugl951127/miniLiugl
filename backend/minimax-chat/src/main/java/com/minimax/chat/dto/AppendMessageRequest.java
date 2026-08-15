package com.minimax.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppendMessageRequest {

    @NotBlank
    private String role;          // user / assistant / system / tool

    @NotBlank
    private String content;

    private Integer tokens;
    private String finishReason;
    private String errorMessage;
}
