package com.example.top_hog_server.model;

import lombok.Data;

import java.util.LinkedList;

/**
 * 代表游戏桌面上的一个牌列（行）。
 * "谁是猪头王"游戏通常有4行牌。
 */
@Data
public class GameRow {
    // 使用 LinkedList 存储牌，因为牌的添加总是在末尾，且可能需要查看末尾的牌
    private LinkedList<Card> cards = new LinkedList<>();
    private final int MAX_CARDS_IN_ROW = 5; // 一行中最多能放5张牌，第6张触发惩罚

    /**
     * 判断一张牌是否可以被添加到当前牌列的末尾。
     * 规则：新牌的数字必须大于当前牌列末尾牌的数字。
     *
     * @param card 要判断的牌
     * @return 如果可以添加则返回 true，否则返回 false
     */
    public boolean canAddCard(Card card) {
        if (cards.isEmpty()) {
            return true; // 任何牌都可以作为空行的第一张牌 (虽然游戏开始时行会有初始牌)
        }
        return card.getNumber() > cards.getLast().getNumber();
    }

    /**
     * 向牌列末尾添加一张牌。
     *
     * @param card 要添加的牌
     */
    public void addCard(Card card) {
        cards.add(card);
    }

    /**
     * 判断当前牌列是否已满（达到5张牌）。
     *
     * @return 如果牌列已满则返回 true，否则返回 false
     */
    public boolean isFull() {
        return cards.size() >= MAX_CARDS_IN_ROW;
    }

    /**
     * 当玩家出的牌是该行的第6张牌时，玩家必须拿走该行已有的5张牌，
     * 然后该玩家出的牌成为该行新的第一张牌。
     *
     * @param newCard 玩家出的导致拿牌的牌，将成为该行新的起始牌
     * @return 被拿走的牌列表
     */
    public LinkedList<Card> takeRowAndReplace(Card newCard) {
        LinkedList<Card> takenCards = new LinkedList<>(cards); // 复制当前行中的牌作为被拿走的牌
        cards.clear(); // 清空当前行
        cards.add(newCard); // 将新牌作为该行的新起始牌
        return takenCards;
    }

    /**
     * 获取牌列中的最后一张牌。
     *
     * @return 最后一张牌，如果牌列为空则返回 null
     */
    public Card getLastCard() {
        return cards.isEmpty() ? null : cards.getLast();
    }

    /**
     * 计算当前牌列中所有牌的猪头数总和。
     *
     * @return 猪头数总和
     */
    public int getBullheadSum() {
        return cards.stream().mapToInt(Card::getBullheads).sum();
    }
}