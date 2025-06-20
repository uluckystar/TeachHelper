package com.teachhelper.websocket;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 使用 TextWebSocketHandler 是一个更好的选择，它是 WebSocketHandler 的实现
@Component
public class TaskWebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskWebSocketHandler.class);
    
    // 用于存储会话的 Map
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getUserId(session);
        sessions.put(userId, session);
        System.out.println("WebSocket connection established for user: " + userId);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 处理接收到的消息
        String payload = message.getPayload();
        System.out.println("Received message: " + payload);
        
        // 这里可以添加消息处理逻辑
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = getUserId(session);
        sessions.remove(userId);
        System.out.println("WebSocket connection closed for user: " + userId);
    }
    
    // 从会话中获取用户ID的辅助方法
    private String getUserId(WebSocketSession session) {
        // 实现获取用户ID的逻辑，例如从session属性中获取
        return session.getId(); // 简单示例，使用会话ID作为用户ID
    }
    
    // 发送消息给指定用户的方法
    public void sendMessageToUser(String userId, String message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                logger.error("Failed to send message to user {}: {}", userId, e.getMessage());
            }
        }
    }
    
    // 广播消息的方法
    public void broadcast(String message) {
        sessions.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    logger.error("Failed to send broadcast message: {}", e.getMessage());
                }
            }
        });
    }
    
    // 广播任务更新的方法
    public void broadcastTaskUpdate(String taskId, String status, Integer progress, String type, Object result) {
        try {
            String message = String.format(
                "{\"taskId\":\"%s\",\"status\":\"%s\",\"progress\":%d,\"type\":\"%s\",\"timestamp\":%d}",
                taskId, status, progress != null ? progress : 0, type, System.currentTimeMillis()
            );
            broadcast(message);
        } catch (Exception e) {
            System.err.println("广播任务更新失败: " + e.getMessage());
        }
    }
}
