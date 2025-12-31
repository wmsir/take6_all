package com.example.top_hog_server.payload.dto.response;

import com.example.top_hog_server.model.GameRoom;
import com.example.top_hog_server.model.GameState;
import com.example.top_hog_server.model.Player;
import com.example.top_hog_server.model.Card;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class GameRoomDTO {
    private String roomId;
    private String roomName;
    private GameState gameState;
    private int maxPlayers;
    private int maxRounds;
    private int targetScore;
    private Map<String, PlayerDTO> players;
    private int currentRound;
    private int currentTurnNumber;

    private List<GameRowDTO> rows;
    private Map<String, Card> playedCardsThisTurn;
    private String winnerDisplayName;
    private String playerChoosingRowSessionId;

    public static GameRoomDTO from(GameRoom room, String requestingUserId) {
        GameRoomDTO dto = new GameRoomDTO();
        dto.setRoomId(room.getRoomId());
        dto.setRoomName(room.getRoomName());
        dto.setGameState(room.getGameState());
        dto.setMaxPlayers(room.getMaxPlayers());
        dto.setMaxRounds(room.getMaxRounds());
        dto.setTargetScore(room.getTargetScore());
        dto.setCurrentRound(room.getCurrentRound());
        dto.setCurrentTurnNumber(room.getCurrentTurnNumber());
        dto.setPlayerChoosingRowSessionId(room.getPlayerChoosingRowSessionId());

        // Transform GameRow to GameRowDTO to include display flags
        List<GameRowDTO> rowDTOs = new ArrayList<>();
        if (room.getRows() != null) {
            for (com.example.top_hog_server.model.GameRow row : room.getRows()) {
                rowDTOs.add(GameRowDTO.from(row));
            }
        }
        dto.setRows(rowDTOs);

        dto.setWinnerDisplayName(room.getWinnerDisplayName());

        // Populate playedCardsThisTurn based on visibility rules
        // In this game, usually played cards are revealed only after everyone plays (Showdown)
        // OR they are revealed as they are played?
        // Standard 6 nimmt!: Cards are played face down, then revealed simultaneously.
        // If we want to support "Showdown", we should only send the full map when ALL have played.
        // However, for simplicity and to answer the user's request about "notification after all played":
        // We will include the map. But maybe hide values if turn not complete?
        // Code in GameLogicService broadcasts "X played".
        // If we send the card value immediately, other players see it.
        // To strictly follow rules (Face Down), we should hide the card value until everyone played.

        // Let's implement a safe view:
        Map<String, Card> visiblePlayedCards = new HashMap<>();
        boolean allPlayed = room.getPlayers().size() > 0 && room.getPlayedCardsThisTurn().size() == room.getPlayers().size();

        // If game is over or we are processing turn, cards should be visible
        boolean reveal = allPlayed || room.getGameState() == GameState.PROCESSING_TURN || room.getGameState() == GameState.WAITING_FOR_PLAYER_CHOICE || room.getGameState() == GameState.ROUND_OVER || room.getGameState() == GameState.GAME_OVER;

        for (Map.Entry<String, Card> entry : room.getPlayedCardsThisTurn().entrySet()) {
            // Check if this card belongs to the requesting user
            boolean isOwnCard = false;
            Player p = room.getPlayers().get(entry.getKey());
            if (p != null && p.getUserId() != null && requestingUserId != null && String.valueOf(p.getUserId()).equals(requestingUserId)) {
                isOwnCard = true;
            }

            if (reveal || isOwnCard) { // Show own card always, others only if reveal
                 visiblePlayedCards.put(entry.getKey(), entry.getValue());
            } else {
                 // Hide card value (e.g. use a dummy card or special flag)
                 // For now, let's send a "Face Down" card representation if needed, or just not send it?
                 // If we don't send it, frontend doesn't know they played.
                 // Better to send a card with number -1 or similar.
                 visiblePlayedCards.put(entry.getKey(), new Card(-1, 0));
            }
        }
        // Wait, current logic in GameLogicService handles "X played" message.
        // If we want "Showdown" notification, we need to ensure the LAST broadcast (when allPlayed=true) sends the real cards.
        // And previous broadcasts send hidden cards.

        // However, looking at the code, GameLogicService broadcasts "X played" immediately.
        // The user's question implies they WANT to see the situation when all played.

        // Let's stick to the plan: Expose the map.
        // BUT, we must handle the "Face Down" rule if we care about cheating/gameplay.
        // Assuming Standard Rules: Face Down.

        // Refined logic:
        // Use the reveal logic above.
        dto.setPlayedCardsThisTurn(visiblePlayedCards);

        Map<String, PlayerDTO> playerDTOs = new HashMap<>();
        for (Map.Entry<String, Player> entry : room.getPlayers().entrySet()) {
            Player p = entry.getValue();
            // User ID matching logic:
            // room keys are sessionIds. requestingUserId is usually userId (Long) as String?
            // In GameLogicService, we pass: String userIdStr = p.getUserId() != null ? String.valueOf(p.getUserId()) : null;
            // So we compare against p.getUserId().
            boolean isSelf = false;
             if (requestingUserId != null && p.getUserId() != null) {
                isSelf = String.valueOf(p.getUserId()).equals(requestingUserId);
            } else if (requestingUserId == null) {
                 // Should not happen for authenticated user, but safe fallback
                 isSelf = false;
            }

            playerDTOs.put(entry.getKey(), PlayerDTO.from(p, isSelf));
        }
        dto.setPlayers(playerDTOs);

        return dto;
    }

    @Data
    public static class GameRowDTO {
        private List<CardViewDTO> cards;
        private int totalBullheads;
        private boolean isDanger;

        public static GameRowDTO from(com.example.top_hog_server.model.GameRow row) {
            GameRowDTO dto = new GameRowDTO();
            dto.setTotalBullheads(row.getBullheadSum());
            dto.setDanger(row.getCards().size() >= 5); // Example danger logic: nearly full

            List<CardViewDTO> cardDTOs = new ArrayList<>();
            int index = 0;
            for (Card c : row.getCards()) {
                CardViewDTO cardView = new CardViewDTO();
                cardView.setNumber(c.getNumber());
                cardView.setBullheads(c.getBullheads());
                cardView.setHead(index == 0);
                cardView.setFifth(index == 4);
                cardView.setDanger(index >= 5);
                cardDTOs.add(cardView);
                index++;
            }
            dto.setCards(cardDTOs);
            return dto;
        }
    }

    @Data
    public static class CardViewDTO {
        private int number;
        private int bullheads;
        private boolean isHead;
        private boolean isFifth;
        private boolean isDanger;
    }
}
