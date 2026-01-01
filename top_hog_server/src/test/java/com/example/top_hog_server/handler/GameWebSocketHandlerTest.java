package com.example.top_hog_server.handler;

import com.example.top_hog_server.model.GameRoom;
import com.example.top_hog_server.model.Player;
import com.example.top_hog_server.model.User;
import com.example.top_hog_server.repository.UserRepository;
import com.example.top_hog_server.service.GameLogicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GameWebSocketHandlerTest {

    @Mock
    private GameLogicService gameLogicService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WebSocketSession session;

    private GameWebSocketHandler gameWebSocketHandler;
    private ObjectMapper objectMapper;

    // Must match UserHandshakeInterceptorRaw.USER_IDENTIFIER_SESSION_KEY
    private static final String USER_IDENTIFIER_SESSION_KEY = "userIdentifier_websocket_session_attr";

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        gameWebSocketHandler = new GameWebSocketHandler(gameLogicService, userRepository, objectMapper);
    }

    @Test
    public void testToggleHosting() throws Exception {
        // Arrange
        String roomId = "123";
        String sessionId = "s1";
        Long userId = 1L;

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("type", "toggleHosting");
        messageMap.put("roomId", roomId);
        messageMap.put("isHosting", true);
        messageMap.put("userIdentifier", userId);

        String payload = objectMapper.writeValueAsString(messageMap);
        TextMessage message = new TextMessage(payload);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(USER_IDENTIFIER_SESSION_KEY, userId);

        when(session.getAttributes()).thenReturn(attributes);
        when(session.getId()).thenReturn(sessionId);

        // Act
        gameWebSocketHandler.handleTextMessage(session, message);

        // Assert
        verify(gameLogicService).togglePlayerAutoPlay(roomId, sessionId);
    }

    @Test
    public void testToggleAutoPlay() throws Exception {
        // Arrange
        String roomId = "123";
        String sessionId = "s1";
        Long userId = 1L;

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("type", "toggleAutoPlay");
        messageMap.put("roomId", roomId);

        String payload = objectMapper.writeValueAsString(messageMap);
        TextMessage message = new TextMessage(payload);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(USER_IDENTIFIER_SESSION_KEY, userId);

        when(session.getAttributes()).thenReturn(attributes);
        when(session.getId()).thenReturn(sessionId);

        // Act
        gameWebSocketHandler.handleTextMessage(session, message);

        // Assert
        verify(gameLogicService).togglePlayerAutoPlay(roomId, sessionId);
    }
}
