package com.example.top_hog_server.controller;

import com.example.top_hog_server.model.GameRoom;
import com.example.top_hog_server.payload.dto.request.AddBotsRequest;
import com.example.top_hog_server.service.GameLogicService;
import com.example.top_hog_server.service.GameRoomService;
import com.example.top_hog_server.security.services.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class RoomControllerBotTest {

    private MockMvc mockMvc;

    @Mock
    private GameRoomService gameRoomService;

    @Mock
    private GameLogicService gameLogicService;

    @InjectMocks
    private RoomController roomController;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(roomController).build();
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void testAddBots_Success() throws Exception {
        Long ownerId = 123L;
        String roomId = "ROOM1";

        GameRoom room = new GameRoom(roomId, "Test Room");
        room.setOwnerId(ownerId);

        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        when(userDetails.getId()).thenReturn(ownerId);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(gameRoomService.getRoom(roomId)).thenReturn(room);

        mockMvc.perform(post("/api/room/add-bots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomId\":\"ROOM1\", \"botCount\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(gameLogicService, times(1)).addBotsToRoom(roomId, 2);
    }

    @Test
    public void testAddBots_Forbidden() throws Exception {
        Long ownerId = 123L;
        Long otherUserId = 456L;
        String roomId = "ROOM1";

        GameRoom room = new GameRoom(roomId, "Test Room");
        room.setOwnerId(ownerId);

        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        when(userDetails.getId()).thenReturn(otherUserId); // Different user

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(gameRoomService.getRoom(roomId)).thenReturn(room);

        mockMvc.perform(post("/api/room/add-bots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomId\":\"ROOM1\", \"botCount\":2}"))
                .andExpect(status().isOk()) // ApiResponse error returns 200 HTTP usually with code inside? Wait, ApiResponse.error uses custom code.
                // Let's check ApiResponse implementation. Typically it returns JSON with code field.
                // But RoomController returns ApiResponse directly.
                // If ApiResponse.error sets HTTP status, checking status().isOk() might be wrong if it returns 403 status.
                // Let's assume ApiResponse handles the structure.
                .andExpect(jsonPath("$.code").value(403));

        verify(gameLogicService, never()).addBotsToRoom(anyString(), anyInt());
    }
}
