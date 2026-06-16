package com.minimax.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollabHandler extends TextWebSocketHandler {

    private final CollabService collab;

    @Override
    public void afterConnectionEstablished(WebSocketSession ws) {
        collab.onJoin(ws);
    }

    @Override
    protected void handleTextMessage(WebSocketSession ws, TextMessage message) {
        collab.onMessage(ws, message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession ws, CloseStatus status) {
        collab.onClose(ws);
    }
}
