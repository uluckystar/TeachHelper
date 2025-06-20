package com.teachhelper.websocket;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 测试WebSocket依赖是否正确
 */
public class WebSocketDependencyTest {

    @Test
    public void testWebSocketDependenciesAvailable() {
        // 测试WebSocket相关类是否可用
        assertNotNull(WebSocketSession.class);
        assertNotNull(TextMessage.class);
        assertNotNull(CloseStatus.class);
        assertNotNull(TextWebSocketHandler.class);
    }
}
