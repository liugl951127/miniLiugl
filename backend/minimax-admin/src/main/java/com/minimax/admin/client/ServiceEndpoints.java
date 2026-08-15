package com.minimax.admin.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 各服务 URL 集中配置 (V3.5.25+)
 *
 * <p>V3.5.25 端口修正:</p>
 * <ul>
 *   <li>model: 8083 → 8084 (V3.5.18 prompt 合并到 model 时漏改)</li>
 *   <li>memory: 8084 → 8082 (V3.5.18 memory 合并到 chat)</li>
 *   <li>function: 8086 → 8093 (V3.5.18 function 合并到 pipeline)</li>
 *   <li>prompt 8081: 删除 (V3.5.18 合并到 model 8084)</li>
 *   <li>新增: pipeline 8093, agent 8088, monitor 8089, multimodal 8087, ws 8095, analytics 8092</li>
 * </ul>
 *
 * <p>所有端点跟 V3.5.18 合并后的 13 微服务端口一致</p>
 */
@Component
public class ServiceEndpoints {

    @Value("${minimax.admin.services.auth:http://minimax-auth:8081}")
    private String auth;
    @Value("${minimax.admin.services.chat:http://minimax-chat:8082}")
    private String chat;
    @Value("${minimax.admin.services.model:http://minimax-model:8084}")
    private String model;
    @Value("${minimax.admin.services.rag:http://minimax-rag:8085}")
    private String rag;
    @Value("${minimax.admin.services.multimodal:http://minimax-multimodal:8087}")
    private String multimodal;
    @Value("${minimax.admin.services.agent:http://minimax-agent:8088}")
    private String agent;
    @Value("${minimax.admin.services.monitor:http://minimax-monitor:8089}")
    private String monitor;
    @Value("${minimax.admin.services.pipeline:http://minimax-pipeline:8093}")
    private String pipeline;
    @Value("${minimax.admin.services.ai:http://minimax-ai:8094}")
    private String ai;
    @Value("${minimax.admin.services.ws:http://minimax-ws:8095}")
    private String ws;
    @Value("${minimax.admin.services.analytics:http://minimax-analytics:8092}")
    private String analytics;

    public String auth()        { return auth; }
    public String chat()        { return chat; }
    public String model()       { return model; }
    public String rag()         { return rag; }
    public String multimodal()  { return multimodal; }
    public String agent()       { return agent; }
    public String monitor()     { return monitor; }
    /** V3.5.18+ memory 已合并到 chat, 旧代码调 memory() 仍返 chat URL */
    public String memory()      { return chat; }
    /** V3.5.18+ function 已合并到 pipeline, 旧代码调 function() 仍返 pipeline URL */
    public String function()    { return pipeline; }
    public String pipeline()    { return pipeline; }
    public String ai()          { return ai; }
    public String ws()          { return ws; }
    public String analytics()   { return analytics; }
    /** V3.5.18+ prompt 已合并到 model, 旧代码调 prompt() 仍返 model URL */
    public String prompt()      { return model; }
}
