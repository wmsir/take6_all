// src/main/java/com/example/top_hog_server/model/GameRoom.java
package com.example.top_hog_server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameRoom {
    @Id
    private String roomId; // 房间ID
    private String roomName; // 房间名称
    private int maxPlayers = 6; // 最大玩家数, default changed to 6 per requirements

    private int maxRounds = 3; // 最大局数
    private int targetScore = 66; // 目标分数
    private boolean isPrivate = false; // 是否私密房间

    @JsonIgnore // Do not expose password in JSON serialization typically, unless explicit
    private String password; // 房间密码

    private Long ownerId; // 房主ID

    private int currentRound = 1;

    @Enumerated(EnumType.STRING)
    private GameState gameState = GameState.WAITING; // 当前游戏状态

    @Enumerated(EnumType.STRING)
    private GameType gameType = GameType.TOP_HOG; // 游戏类型，默认为猪头王

    @Transient
    private Map<String, Player> players = new ConcurrentHashMap<>(); // sessionId -> Player 对象

    @Transient
    private List<GameRow> rows = new ArrayList<>(4);

    @Transient
    private List<Card> deck; // 当前游戏的牌堆

    @Transient
    private Map<String, Card> playedCardsThisTurn = new ConcurrentHashMap<>(); // 本“手”牌中，每个玩家已打出的牌

    @Transient
    private int currentTurnNumber = 0; // 当前是第几“手”出牌 (一轮游戏共10“手”出牌)

    @Transient
    private String playerChoosingRowSessionId;
    @Transient
    private Card cardPendingChoice;
    @Transient
    private List<Map.Entry<String, Card>> cardsRemainingInTrick;

    @Transient
    private Set<String> playersRequestedNewGame = new HashSet<>();

    @Transient
    private String winnerDisplayName;

    // 为会员提示功能添加的字段: 存储所有玩家的手牌（仅供AI提示逻辑使用）
    @Transient
    private Map<String, List<Card>> allPlayerHandsForAI;

    @Transient
    private long createdAtTimestamp = System.currentTimeMillis();

    public GameRoom(String roomId, String roomName) {
        this.roomId = roomId;
        this.roomName = roomName;
        initializeRowsStructure();
    }

    /**
     * 初始化4个空的牌列结构。
     */
    private void initializeRowsStructure() {
        if (this.rows == null || this.rows.isEmpty()) {
            this.rows = new ArrayList<>(4);
            for (int i = 0; i < 4; i++) {
                this.rows.add(new GameRow());
            }
        }
    }

    /**
     * 清空所有牌列中的牌。
     */
    public void clearAllRows() {
        if (this.rows != null) {
            for(GameRow row : this.rows) {
                if (row != null) {
                    row.getCards().clear();
                }
            }
        }
    }

    /**
     * 向房间添加玩家。
     * @param player 要添加的玩家对象
     * @throws IllegalStateException 如果房间已满或游戏正在进行中（非WAITING或GAME_OVER状态）
     */
    public void addPlayer(Player player) {
        if (player == null || player.getSessionId() == null) {
            System.err.println("尝试添加空的玩家或没有会话ID的玩家。");
            return;
        }
        if (players.size() < maxPlayers && (gameState == GameState.WAITING || gameState == GameState.GAME_OVER) ) {
            players.put(player.getSessionId(), player);
        } else if (players.containsKey(player.getSessionId())) {
            players.put(player.getSessionId(), player);
        }
        else if (players.size() >= maxPlayers && gameState != GameState.WAITING && gameState != GameState.GAME_OVER) {
            throw new IllegalStateException("房间已满或游戏正在进行中，无法加入！");
        } else if (players.size() >= maxPlayers) {
            throw new IllegalStateException("房间已满，无法加入！");
        } else {
            throw new IllegalStateException("游戏正在进行中，新玩家无法加入！");
        }
    }

    /**
     * 从房间移除玩家。
     * @param sessionId 要移除的玩家的会话ID
     */
    public void removePlayer(String sessionId) {
        if (sessionId == null) return;
        Player removedPlayer = players.remove(sessionId);
        if (removedPlayer != null) {
            playedCardsThisTurn.remove(sessionId);
            playersRequestedNewGame.remove(sessionId);
        }
    }

    // For listing rooms API, to show current players count
    public int getCurrentPlayers() {
        return players != null ? players.size() : 0;
    }

    @PostLoad
    private void onPostLoad() {
        if (this.players == null) this.players = new ConcurrentHashMap<>();
        if (this.rows == null || this.rows.isEmpty()) {
            this.rows = new ArrayList<>(4);
            initializeRowsStructure();
        }
        if (this.playedCardsThisTurn == null) this.playedCardsThisTurn = new ConcurrentHashMap<>();
        if (this.playersRequestedNewGame == null) this.playersRequestedNewGame = new HashSet<>();
    }

    public void resetForNewGame() {
        this.getPlayers().values().forEach(Player::resetForNewGame);
        this.setDeck(new ArrayList<>());
        this.clearAllRows();
        this.getPlayedCardsThisTurn().clear();
        this.setCurrentTurnNumber(0);
        this.setPlayerChoosingRowSessionId(null);
        this.setCardPendingChoice(null);
        this.setCardsRemainingInTrick(null);
        this.getPlayersRequestedNewGame().clear();
        this.setWinnerDisplayName(null);
        this.setAllPlayerHandsForAI(null); // 新游戏时清空
        this.setGameState(GameState.WAITING);
        // Do not reset rounds or target score
    }
}
