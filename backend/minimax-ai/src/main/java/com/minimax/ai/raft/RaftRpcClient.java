package com.minimax.ai.raft;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Raft RPC 客户端 (V3.5.0)
 *
 * <p>抽象 RPC 层, 生产对接:
 *   - HTTP (e.g. spring-webClient)
 *   - gRPC
 *   - Netty
 *
 * <p>沙箱模式: in-process 模拟 (单节点测试用)
 * 真实生产: 注入远程 peer 地址, 通过 HTTP 调对方 /raft/* API
 */
@Slf4j
@Service
public class RaftRpcClient {

    /**
     * 广播投票请求
     *
     * @return peerId -> 响应
     */
    public Map<String, RaftRpc.VoteResponse> broadcastVote(RaftRpc.RequestVote req) {
        // 真实生产: HTTP POST peer/v1/raft/vote
        // 单节点模式: 空 map
        log.debug("[raft] broadcastVote: term={}, candidate={}", req.getTerm(), req.getCandidateId());
        return Collections.emptyMap();
    }

    /**
     * 发送 AppendEntries (单 peer)
     */
    public RaftRpc.AppendResponse sendAppend(String peer, RaftRpc.AppendEntries req) {
        // 真实生产: HTTP POST peer/v1/raft/append
        return new RaftRpc.AppendResponse(req.getTerm(), true, 0L, peer);
    }
}
