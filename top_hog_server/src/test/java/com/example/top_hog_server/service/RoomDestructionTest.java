package com.example.top_hog_server.service;

import com.example.top_hog_server.model.GameRoom;
import com.example.top_hog_server.model.GameState;
import com.example.top_hog_server.model.Player;
import com.example.top_hog_server.repository.GameHistoryRepository;
import com.example.top_hog_server.repository.UserRepository;
import com.example.top_hog_server.handler.GameWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomDestructionTest {

    @Mock
    private GameRoomService gameRoomService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameHistoryRepository gameHistoryRepository;

    @Mock
    private GameWebSocketHandler gameWebSocketHandler;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private BotProfileService botProfileService;

    private GameLogicService gameLogicService;

    @BeforeEach
    public void setup() {
        gameLogicService = new GameLogicService(
                gameRoomService,
                userRepository,
                gameHistoryRepository,
                gameWebSocketHandler,
                taskScheduler,
                botProfileService
        );
    }

    @Test
    public void testRoomDestroyedWhenLastHumanPlayerLeaves() {
        // Create a room with one human player and bots
        GameRoom room = new GameRoom("TEST1", "Test Room");
        room.setGameState(GameState.WAITING);

        // Add human player
        Player humanPlayer = new Player("session1", 1L, "Human Player", 0);
        humanPlayer.setRobot(false);
        room.getPlayers().put("session1", humanPlayer);

        // Add bot players
        Player bot1 = new Player("bot1", null, "Bot 1", 0);
        bot1.setRobot(true);
        room.getPlayers().put("bot1", bot1);

        Player bot2 = new Player("bot2", null, "Bot 2", 0);
        bot2.setRobot(true);
        room.getPlayers().put("bot2", bot2);

        // Mock the room service to return the room
        when(gameRoomService.getRoom("TEST1")).thenReturn(room);

        // Human player requests to leave
        gameLogicService.playerRequestsLeave("TEST1", 1L);

        // Verify that removeRoom was called (room should be destroyed)
        verify(gameRoomService, times(1)).removeRoom("TEST1");
    }

    @Test
    public void testRoomNotDestroyedWhenMultipleHumansAndOneLeaves() {
        // Create a room with two human players
        GameRoom room = new GameRoom("TEST2", "Test Room");
        room.setGameState(GameState.WAITING);

        // Add first human player
        Player humanPlayer1 = new Player("session1", 1L, "Human Player 1", 0);
        humanPlayer1.setRobot(false);
        room.getPlayers().put("session1", humanPlayer1);

        // Add second human player
        Player humanPlayer2 = new Player("session2", 2L, "Human Player 2", 0);
        humanPlayer2.setRobot(false);
        room.getPlayers().put("session2", humanPlayer2);

        // Mock the room service to return the room
        when(gameRoomService.getRoom("TEST2")).thenReturn(room);

        // First human player requests to leave
        gameLogicService.playerRequestsLeave("TEST2", 1L);

        // Verify that removeRoom was NOT called (still one human left)
        verify(gameRoomService, never()).removeRoom("TEST2");
    }

    @Test
    public void testRoomDestroyedWhenAllPlayersLeave() {
        // Create a room with one human player
        GameRoom room = new GameRoom("TEST3", "Test Room");
        room.setGameState(GameState.WAITING);

        // Add human player
        Player humanPlayer = new Player("session1", 1L, "Human Player", 0);
        humanPlayer.setRobot(false);
        room.getPlayers().put("session1", humanPlayer);

        // Mock the room service to return the room
        when(gameRoomService.getRoom("TEST3")).thenReturn(room);

        // Human player requests to leave
        gameLogicService.playerRequestsLeave("TEST3", 1L);

        // Verify that removeRoom was called (room is empty)
        verify(gameRoomService, times(1)).removeRoom("TEST3");
    }

    @Test
    public void testRoomDestroyedInGameOverStateWhenLastHumanLeaves() {
        // Create a room in GAME_OVER state with one human player and bots
        GameRoom room = new GameRoom("TEST4", "Test Room");
        room.setGameState(GameState.GAME_OVER);

        // Add human player
        Player humanPlayer = new Player("session1", 1L, "Human Player", 0);
        humanPlayer.setRobot(false);
        room.getPlayers().put("session1", humanPlayer);

        // Add bot players
        Player bot1 = new Player("bot1", null, "Bot 1", 0);
        bot1.setRobot(true);
        room.getPlayers().put("bot1", bot1);

        // Mock the room service to return the room
        when(gameRoomService.getRoom("TEST4")).thenReturn(room);

        // Human player requests to leave after game ends
        gameLogicService.playerRequestsLeave("TEST4", 1L);

        // Verify that removeRoom was called (last human left)
        verify(gameRoomService, times(1)).removeRoom("TEST4");
    }
}
