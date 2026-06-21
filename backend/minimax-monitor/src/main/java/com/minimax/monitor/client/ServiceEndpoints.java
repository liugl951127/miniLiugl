package com.minimax.monitor.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * monitor 跨服务调用端点 (V5.10).
 * 用于 metrics 转发 / 健康检查联动.
 */
@Component
public class ServiceEndpoints {

    @Value("${minimax.monitor.services.gateway:http://localhost:8080}")
    private String gateway;
    @Value("${minimax.monitor.services.auth:http://localhost:8081}")
    private String auth;
    @Value("${minimax.monitor.services.chat:http://localhost:8082}")
    private String chat;
    @Value("${minimax.monitor.services.model:http://localhost:8083}")
    private String model;
    @Value("${minimax.monitor.services.memory:http://localhost:8084}")
    private String memory;
    @Value("${minimax.monitor.services.rag:http://localhost:8085}")
    private String rag;
    @Value("${minimax.monitor.services.function:http://localhost:8086}")
    private String function;
    @Value("${minimax.monitor.services.agent:http://localhost:8090}")
    private String agent;
    @Value("${minimax.monitor.services.prompt:http://localhost:8091}")
    private String prompt;
    @Value("${minimax.monitor.services.multimodal:http://localhost:8088}")
    private String multimodal;
    @Value("${minimax.monitor.services.admin:http://localhost:8087}")
    private String admin;
    @Value("${minimax.monitor.services.ws:http://localhost:8095}")
    private String ws;

    public String gateway()    { return gateway; }
    public String auth()       { return auth; }
    public String chat()       { return chat; }
    public String model()      { return model; }
    public String memory()     { return memory; }
    public String rag()        { return rag; }
    public String function()   { return function; }
    public String agent()      { return agent; }
    public String prompt()     { return prompt; }
    public String multimodal() { return multimodal; }
    public String admin()      { return admin; }
    public String ws()         { return ws; }

    /** 根据服务名返回 URL (V5.10 Prometheus 转发) */
    public String resolve(String service) {
        if (service == null) return null;
        return switch (service) {
            case "minimax-gateway"   -> gateway();
            case "minimax-auth"      -> auth();
            case "minimax-chat"      -> chat();
            case "minimax-model"     -> model();
            case "minimax-memory"    -> memory();
            case "minimax-rag"       -> rag();
            case "minimax-function"  -> function();
            case "minimax-agent"     -> agent();
            case "minimax-prompt"    -> prompt();
            case "minimax-multimodal"-> multimodal();
            case "minimax-admin"     -> admin();
            case "minimax-ws"        -> ws();
            default -> null;
        };
    }
}
