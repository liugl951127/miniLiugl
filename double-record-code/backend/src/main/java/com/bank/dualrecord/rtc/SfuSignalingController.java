package com.bank.dualrecord.rtc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebRTC SFU 信令服务(Spring WebSocket)
 *
 * <p>简化:不实际做媒体转发(用 mediasoup/LiveKit)
 * <p>只做房间管理 + 信令转发
 *
 * <p>生产应部署独立的 SFU 服务:
 * <ul>
 *   <li>mediasoup(C++)
 *   <li>LiveKit(Go)
 *   <li>Janus(C)
 *   <li>ZLMediaKit(C++)
 * </ul>
 */
@Slf4j
@Controller
public class SfuSignalingController {

    private final SimpMessagingTemplate messaging;
    private final Map<String, Map<String, String>> rooms = new ConcurrentHashMap<>();

    public SfuSignalingController(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    /**
     * 加入房间
     */
    @MessageMapping("/rtc/join")
    public void onJoin(JoinMessage msg, Principal user) {
        log.info("用户加入: roomId={}, userId={}, role={}", msg.roomId, msg.userId, msg.role);

        rooms.computeIfAbsent(msg.roomId, k -> new ConcurrentHashMap<>())
            .put(msg.userId, msg.role);

        // 通知房间内其他人
        messaging.convertAndSend("/topic/rtc/" + msg.roomId, Map.of(
            "type", "peer-joined",
            "userId", msg.userId,
            "role", msg.role
        ));
    }

    /**
     * 离开房间
     */
    @MessageMapping("/rtc/leave")
    public void onLeave(JoinMessage msg) {
        log.info("用户离开: roomId={}, userId={}", msg.roomId, msg.userId);
        Map<String, String> room = rooms.get(msg.roomId);
        if (room != null) room.remove(msg.userId);

        messaging.convertAndSend("/topic/rtc/" + msg.roomId, Map.of(
            "type", "peer-left",
            "userId", msg.userId
        ));
    }

    /**
     * SDP Offer 转发
     */
    @MessageMapping("/rtc/offer")
    @SendTo("/topic/rtc/room")
    public SignalingMessage onOffer(SignalingMessage msg) {
        log.debug("SDP Offer: from={} to={}", msg.from, msg.to);
        return msg;
    }

    /**
     * SDP Answer 转发
     */
    @MessageMapping("/rtc/answer")
    @SendTo("/topic/rtc/room")
    public SignalingMessage onAnswer(SignalingMessage msg) {
        log.debug("SDP Answer: from={} to={}", msg.from, msg.to);
        return msg;
    }

    /**
     * ICE 候选转发
     */
    @MessageMapping("/rtc/ice")
    @SendTo("/topic/rtc/room")
    public SignalingMessage onIceCandidate(SignalingMessage msg) {
        log.debug("ICE Candidate: from={} to={}", msg.from, msg.to);
        return msg;
    }

    /**
     * 加入消息
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JoinMessage {
        @JsonProperty("type")
        private String type;
        @JsonProperty("roomId")
        private String roomId;
        @JsonProperty("userId")
        private String userId;
        @JsonProperty("role")
        private String role;
    }

    /**
     * 信令消息(Offer/Answer/ICE)
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SignalingMessage {
        @JsonProperty("type")
        private String type;
        @JsonProperty("from")
        private String from;
        @JsonProperty("to")
        private String to;
        @JsonProperty("sdp")
        private String sdp;
        @JsonProperty("candidate")
        private Object candidate;
    }
}
