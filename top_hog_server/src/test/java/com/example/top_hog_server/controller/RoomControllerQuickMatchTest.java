package com.example.top_hog_server.controller;

import com.example.top_hog_server.model.GameRoom;
import com.example.top_hog_server.service.GameLogicService;
import com.example.top_hog_server.service.GameRoomService;
import com.example.top_hog_server.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class RoomControllerQuickMatchTest {

    private MockMvc mockMvc;

    @Mock
    private GameRoomService gameRoomService;

    @Mock
    private GameLogicService gameLogicService;

    @InjectMocks
    private RoomController roomController;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(roomController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    public void testQuickMatch_Success() throws Exception {
        String roomId = "ROOM1";
        GameRoom room = new GameRoom(roomId, "Test Room");

        when(gameRoomService.quickMatch()).thenReturn(room);

        mockMvc.perform(post("/api/room/quick-match")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roomId").value(roomId));

        verify(gameRoomService, times(1)).quickMatch();
    }
}
