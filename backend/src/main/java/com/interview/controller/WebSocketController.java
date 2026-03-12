package com.interview.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Client sends: /app/timer
     * Server broadcasts to: /topic/timer/{sessionId}
     */
    @MessageMapping("/timer")
    @SendTo("/topic/timer")
    public Map<String, Object> handleTimer(Map<String, Object> payload) {
        return payload;
    }

    /**
     * Client sends: /app/typing
     * Server broadcasts typing indicator
     */
    @MessageMapping("/typing")
    public void handleTyping(Map<String, Object> payload) {
        String sessionId = (String) payload.get("sessionId");
        messagingTemplate.convertAndSend("/topic/typing/" + sessionId, payload);
    }

    /**
     * Ping-pong for connection health check
     */
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public Map<String, Object> ping(Map<String, Object> payload) {
        return Map.of("pong", true, "time", System.currentTimeMillis());
    }
}
