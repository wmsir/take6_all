// src/main/java/com/example/top_hog_server/model/Player.java
package com.example.top_hog_server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// Player 类不直接作为JPA实体，它是GameRoom中@Transient字段的一部分
// 如果需要持久化Player的某些信息，通常会关联到User实体或单独创建Player记录
// 这里我们假设Player主要用于游戏运行时的内存表示

@Data
@NoArgsConstructor
// JSON序列化时不包含null值的字段
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Player {

    // WebSocket 会话 ID, 用作游戏内临时标识
    private String sessionId;
    // 关联的用户系统中的用户ID (来自User表)
    private Long userId;
    // 玩家的显示名称 (来自User表的nickname或username)
    private String displayName;
    // 用户头像URL
    private String avatarUrl;
    // 玩家当前累积的猪头数
    private int score = 0;

    // 手牌信息总是发送给当前玩家，会员的全图视角由后端逻辑控制数据填充
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private List<Card> hand = new ArrayList<>();

    // 玩家收集到的牌（用于计算猪头）
    private List<Card> collectedCards = new ArrayList<>();

    @JsonProperty("isReady")
    // 玩家是否已准备开始游戏
    private boolean isReady = false;
    @JsonProperty("isTrustee")
    // 玩家是否处于托管状态 (例如断线)
    private boolean isTrustee = false;
    // 游戏结束后，玩家是否请求再来一局
    private boolean requestedNewGame = false;

    @JsonProperty("isHost")
    // 是否为房主
    private boolean isHost = false;

    @JsonProperty("isRobot")
    // 是否为机器人
    private boolean isRobot = false;

    @JsonIgnore
    // 标记玩家是否明确请求离开房间
    private boolean pendingLeave = false;

    // 会员状态，0=非会员，非0表示会员等级或类型。从User对象同步。
    private int vipStatus = 0;

    /**
     * 构造函数
     * @param sessionId WebSocket会话ID
     * @param userId 用户ID
     * @param displayName 显示名称
     * @param vipStatus 会员状态
     */
    public Player(String sessionId, Long userId, String displayName, int vipStatus) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.displayName = displayName;
        // 设置会员状态
        this.vipStatus = vipStatus;
        this.hand = new ArrayList<>();
        this.collectedCards = new ArrayList<>();
        this.score = 0;
        this.isReady = false;
        this.isTrustee = false;
        this.requestedNewGame = false;
        this.isHost = false;
        this.isRobot = false;
        this.pendingLeave = false;
    }

    public Player(String sessionId, String displayName) {
        this(sessionId, null, displayName, 0);
    }


    public void addCardToHand(Card card) {
        if (this.hand == null) {
            this.hand = new ArrayList<>();
        }
        this.hand.add(card);
    }

    public boolean removeCardFromHand(Card cardToRemove) {
        if (this.hand == null) return false;
        return this.hand.removeIf(card -> card.getNumber() == cardToRemove.getNumber());
    }

    public void addCollectedCards(List<Card> cards) {
        if (this.collectedCards == null) {
            this.collectedCards = new ArrayList<>();
        }
        this.collectedCards.addAll(cards);
        // 每次收集牌后更新分数
        updateScore();
    }

    private void updateScore() {
        this.score = 0;
        if (this.collectedCards != null) {
            for (Card card : this.collectedCards) {
                this.score += card.getBullheads();
            }
        }
    }

    /**
     * 为全新游戏重置玩家状态（例如，在所有轮次结束后开始一个新游戏）
     */
    public void resetForNewGame() {
        this.hand.clear();
        this.collectedCards.clear();
        this.score = 0;
        this.isReady = false;
        this.isTrustee = false;
        this.requestedNewGame = false;
        this.pendingLeave = false;
        // vipStatus 通常在玩家加入时从 User 对象获取，新游戏时不重置会员身份本身
        // isHost 也不应该重置，房主身份保持
    }

    /**
     * 为新一轮重置玩家状态（例如，在一局游戏中的10手牌结束后）
     * 通常只清空手牌，分数等保留。
     */
    public void resetForNewRound() {
        // 清空手牌
        this.hand.clear();
        // 可能需要玩家重新确认准备状态
        this.isReady = false;
        // score, collectedCards, is托管, requestedNewGame, vipStatus 在轮次间保留
    }
}
