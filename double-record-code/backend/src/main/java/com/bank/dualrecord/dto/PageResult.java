package com.bank.dualrecord.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 统一分页结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页结果")
public class PageResult<T> implements Serializable {
    @Schema(description = "数据列表")
    private List<T> items;
    @Schema(description = "总数")
    private long total;
    @Schema(description = "当前页")
    private long page;
    @Schema(description = "每页大小")
    private long size;
    @Schema(description = "总页数")
    private long totalPages;
}
