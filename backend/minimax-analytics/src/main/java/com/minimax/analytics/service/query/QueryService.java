package com.minimax.analytics.service.query;

import com.minimax.analytics.dto.QueryRequest;
import com.minimax.analytics.vo.QueryResult;

/**
 * SQL 查询服务接口 (V5.31)
 *
 * 安全执行: SqlSafetyChecker 校验 + HikariDataSource + maxRows
 */
public interface QueryService {

    /** 执行 SELECT, 返回前 N 行 */
    QueryResult execute(QueryRequest request);

    /** 仅 EXPLAIN 不执行 */
    QueryResult explain(QueryRequest request);
}
