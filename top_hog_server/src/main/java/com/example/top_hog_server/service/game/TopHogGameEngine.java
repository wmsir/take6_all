package com.example.top_hog_server.service.game;

import com.example.top_hog_server.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 猪头王 (Top Hog) 游戏引擎实现类。
 * 包含所有与猪头王游戏规则相关的核心逻辑。
 */
@Component
public class TopHogGameEngine implements GameEngine {

    private static final Logger logger = LoggerFactory.getLogger(TopHogGameEngine.class);

    // 完整的初始牌堆，包含1到104号牌
    private static final List<Card> INITIAL_FULL_DECK = new ArrayList<>();

    // 静态初始化牌堆数据，定义每张牌的猪头数
    static {
        for (int i = 1; i <= 104; i++) {
            int bullheads = 1; // 默认1个猪头
            if (i == 55) {
                bullheads = 7; // 55号牌：7个猪头
            } else if (i % 11 == 0) {
                bullheads = 5; // 11的倍数：5个猪头
            } else if (i % 10 == 0) {
                bullheads = 3; // 10的倍数：3个猪头
            } else if (i % 5 == 0 && i % 10 != 0) {
                bullheads = 2; // 5的倍数（非10的倍数）：2个猪头
            }
            INITIAL_FULL_DECK.add(new Card(i, bullheads));
        }
    }

    @Override
    public GameType getGameType() {
        return GameType.TOP_HOG;
    }

    @Override
    public void initializeGame(GameRoom room) {
        logger.info("初始化猪头王游戏房间: {}", room.getRoomId());
        
        // 创建新牌堆并洗牌
        List<Card> newDeck = new ArrayList<>(INITIAL_FULL_DECK);
        Collections.shuffle(newDeck);
        room.setDeck(newDeck);
        
        // 清空桌面所有牌列
        room.clearAllRows();

        // 检查牌堆数量是否足够初始化
        if (room.getDeck().size() < 4) {
             throw new IllegalStateException("牌堆不足以初始化牌列");
        }

        // 给每个牌列发一张起始牌
        for (GameRow gameRow : room.getRows()) {
            gameRow.addCard(room.getDeck().remove(0));
        }
    }

    @Override
    public void startNewRound(GameRoom room) {
        logger.info("开始猪头王新一轮: 房间 {}", room.getRoomId());
        
        // 检查是否有足够的牌给所有玩家发10张
        long activePlayersCount = room.getPlayers().values().stream().filter(p -> !p.isTrustee() || room.getPlayers().size() == 1).count();
        if (activePlayersCount == 0 && !room.getPlayers().isEmpty()) {
            activePlayersCount = room.getPlayers().size(); // 全托管情况
        }
        
        if (room.getDeck().size() < (activePlayersCount * 10)) {
            // 牌不够，无法开始新一轮，此时通常意味着游戏应该结束了，或者需要重新洗牌
            // 在此简单逻辑中，我们认为如果牌不够就不发了，由上层 Service 判断游戏结束
             logger.warn("房间 {} 牌堆不足以开始新一轮", room.getRoomId());
             return;
        }

        Map<String, List<Card>> allHandsForAI = new HashMap<>();
        
        // 给每个玩家发10张牌
        for (Player player : room.getPlayers().values()) {
            player.getHand().clear();
            for (int i = 0; i < 10; i++) {
                if (!room.getDeck().isEmpty()) {
                    player.addCardToHand(room.getDeck().remove(0));
                }
            }
            // 排序手牌
            player.getHand().sort(Comparator.comparingInt(Card::getNumber));
            
            // 记录AI可见手牌
            allHandsForAI.put(player.getSessionId(), new ArrayList<>(player.getHand()));
        }
        room.setAllPlayerHandsForAI(allHandsForAI);
        
        // 重置回合计数
        room.setCurrentTurnNumber(1);
    }

    @Override
    public boolean handlePlayerAction(GameRoom room, WebSocketSession session, String action, Map<String, Object> data) {
         if ("play_card".equals(action)) {
             int cardNumber = (Integer) data.get("cardNumber");
             // 逻辑校验在 Service 层做了一部分，这里主要处理游戏规则内的逻辑
             // 但 TopHog 的出牌只是放到 "playedCardsThisTurn" 里，真正的逻辑在所有人都出牌后
             // 所以这里只是简单的返回 true，表示动作已被记录（Service层已处理）
             return true;
         } else if ("select_row".equals(action)) {
             // 玩家选择收牌行的逻辑，也被 Service 层调用了具体的 handlePlayerTakesRow
             // 在完全重构后，Service 应该调用 engine.handlePlayerAction，然后 engine 内部调用 handlePlayerTakesRow
             // 鉴于目前是渐进式重构，我们假设 Service 还在做这部分协调，或者我们需要把 handlePlayerTakesRow 移进来并公开
             return true;
         }
         return false;
    }
    
    /**
     * 执行实际的“拿走牌列”逻辑
     * @return 收取的猪头数
     */
    public int executeTakeRow(GameRoom room, Player player, int rowIndex, Card newCardForThisRow) {
        if (rowIndex < 0 || rowIndex >= room.getRows().size()) return 0;
        
        GameRow rowToTake = room.getRows().get(rowIndex);
        LinkedList<Card> takenCards = rowToTake.takeRowAndReplace(newCardForThisRow);
        
        player.addCollectedCards(takenCards);
        return takenCards.stream().mapToInt(Card::getBullheads).sum();
    }
    
    /**
     * 计算该牌应该放置的目标行索引。
     * @return 目标行索引，如果必须选行则返回 -1
     */
    public int findTargetRowIndex(GameRoom room, Card card) {
        int targetRowIndex = -1;
        int bestDifference = Integer.MAX_VALUE;

        for (int i = 0; i < room.getRows().size(); i++) {
            GameRow currentRow = room.getRows().get(i);
            Card lastCardInRow = currentRow.getLastCard();
            
            if (lastCardInRow != null && card.getNumber() > lastCardInRow.getNumber()) {
                int diff = card.getNumber() - lastCardInRow.getNumber();
                if (diff < bestDifference) {
                    bestDifference = diff;
                    targetRowIndex = i;
                }
            } else if (lastCardInRow == null) {
                 // 空行逻辑，通常不会发生，除非变种规则
                 if (targetRowIndex == -1 || bestDifference > 100000) {
                     bestDifference = 100000 + i;
                     targetRowIndex = i;
                 }
            }
        }
        return targetRowIndex;
    }
    
    /**
     * 自动选择猪头最少的一行
     */
    public int findRowWithMinBullheads(GameRoom room) {
        int bestRowIndex = 0;
        int minBullheads = Integer.MAX_VALUE;
        for (int i = 0; i < room.getRows().size(); i++) {
            int currentBullheads = room.getRows().get(i).getBullheadSum();
            if (currentBullheads < minBullheads) {
                minBullheads = currentBullheads;
                bestRowIndex = i;
            }
        }
        return bestRowIndex;
    }

    @Override
    public boolean isGameOver(GameRoom room) {
        // 任意玩家分数 >= 66
        return room.getPlayers().values().stream().anyMatch(p -> p.getScore() >= room.getTargetScore());
    }

    @Override
    public Map<String, Integer> calculateFinalScores(GameRoom room) {
        return room.getPlayers().values().stream()
                .collect(Collectors.toMap(Player::getSessionId, Player::getScore));
    }

    @Override
    public Map<String, Object> getGameStateForPlayer(GameRoom room, Player forPlayer) {
        // 这里可以过滤掉对于该玩家不可见的信息，比如其他人的手牌
        // 目前 GameLogicService 是直接序列化 GameRoomDTO，这里可以保留空实现或返回特定 Map
        return new HashMap<>(); // 暂由 Service 层处理 DTO 转换
    }

    @Override
    public void cleanupGame(GameRoom room) {
        // 清理逻辑
    }

    @Override
    public boolean validatePlayerAction(GameRoom room, Player player, String action, Map<String, Object> data) {
        // 校验逻辑
        return true;
    }

    @Override
    public void handlePlayerDisconnect(GameRoom room, Player player) {
        // 特定处理
    }

    @Override
    public void handlePlayerReconnect(GameRoom room, Player player) {
        // 特定处理
    }

    @Override
    public GameConfiguration getGameConfiguration() {
        return null;
    }
    
    /**
     * AI出牌策略
     */
    public Card determineBotPlayCard(Player bot, GameRoom room) {
         // 简单策略：出最小的牌
         return bot.getHand().stream()
                .min(Comparator.comparingInt(Card::getNumber))
                .orElse(null);
    }
}
