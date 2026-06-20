package com.minimax.common.web;

import lombok.Data;

/**
 * 通用分页请求 (V5.4 冗余重构).
 * 替代每个 controller 自己定义 page/size 参数.
 */
@Data
public class PageRequest {

    private Integer page = 1;

    private Integer size = 10;

    public int page() {
        return page == null || page < 1 ? 1 : page;
    }

    public int size() {
        if (size == null || size < 1) return 10;
        if (size > 200) return 200;
        return size;
    }
}