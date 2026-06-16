package com.minimax.admin.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 各服务 URL 集中配置。
 * 生产应从配置中心 / 服务发现获取。
 */
@Component
public class ServiceEndpoints {

    @Value("${minimax.admin.services.auth:http://localhost:8081}")
    private String auth;
    @Value("${minimax.admin.services.chat:http://localhost:8082}")
    private String chat;
    @Value("${minimax.admin.services.model:http://localhost:8083}")
    private String model;
    @Value("${minimax.admin.services.memory:http://localhost:8084}")
    private String memory;
    @Value("${minimax.admin.services.rag:http://localhost:8085}")
    private String rag;
    @Value("${minimax.admin.services.function:http://localhost:8086}")
    private String function;

    public String auth()      { return auth; }
    public String chat()      { return chat; }
    public String model()     { return model; }
    public String memory()    { return memory; }
    public String rag()       { return rag; }
    public String function()  { return function; }
}
