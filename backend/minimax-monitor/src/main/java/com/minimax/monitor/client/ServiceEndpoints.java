package com.minimax.monitor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 服务端点配置 (V3.5.18+)
 *
 * <p>V3.5.18+ 合并: memory → chat, function → pipeline, prompt → model
 * 所以原 minimax-memory 路由现在指向 minimax-chat, 以此类推
 */
@Slf4j
@Component
public class ServiceEndpoints {

    @Value("${minimax.gateway.host:localhost}")
    private String gatewayHost;
    @Value("${minimax.gateway.port:7080}")
    private int gatewayPort;

    public String gateway()  { return base("minimax-gateway"); }
    public String auth()     { return base("minimax-auth"); }
    public String chat()     { return base("minimax-chat"); }
    public String model()    { return base("minimax-model"); }
    public String rag()      { return base("minimax-rag"); }
    public String function() { return base("minimax-pipeline"); }  // V3.5.18+ function → pipeline
    public String agent()    { return base("minimax-agent"); }
    public String prompt()   { return base("minimax-model"); }     // V3.5.18+ prompt → model
    public String memory()   { return base("minimax-chat"); }      // V3.5.18+ memory → chat
    public String multimodal() { return base("minimax-multimodal"); }
    public String admin()    { return base("minimax-admin"); }
    public String ws()       { return base("minimax-ws"); }
    public String monitor()  { return base("minimax-monitor"); }
    public String pipeline() { return base("minimax-pipeline"); }
    public String analytics(){ return base("minimax-analytics"); }

    private String base(String service) {
        return "http://" + service + ":80";
    }

    /**
     * 根据服务名返回 URL (V3.5.18+ 合并后)
     */
    public String resolve(String service) {
        if (service == null) return null;
        return switch (service) {
            case "minimax-gateway"    -> gateway();
            case "minimax-auth"       -> auth();
            case "minimax-chat"       -> chat();
            // V3.5.18+ memory 合并到 chat, 旧名 minimax-memory 路由到 chat
            case "minimax-memory"     -> memory();
            case "minimax-model"      -> model();
            // V3.5.18+ prompt 合并到 model
            case "minimax-prompt"     -> prompt();
            case "minimax-rag"        -> rag();
            // V3.5.18+ function 合并到 pipeline
            case "minimax-function"   -> function();
            case "minimax-pipeline"   -> pipeline();
            case "minimax-agent"      -> agent();
            case "minimax-multimodal" -> multimodal();
            case "minimax-admin"      -> admin();
            case "minimax-ws"         -> ws();
            case "minimax-monitor"    -> monitor();
            case "minimax-analytics"  -> analytics();
            default -> null;
        };
    }
}
