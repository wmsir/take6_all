package com.example.take6server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代表一张游戏卡牌。
 * 包含卡牌数字和牛头数。
 * 使用 Lombok 自动生成 getter, setter, equals, hashCode, toString 和构造函数。
 */
@Data // 自动生成 getter, setter, equals, hashCode, toString
@NoArgsConstructor // 自动生成无参构造函数
@AllArgsConstructor // 自动生成包含所有字段的构造函数
public class Card {
    private int number; // 卡牌上的数字
    private int bullheads; // 卡牌对应的牛头数（惩罚分）

    /**
     * 重写 equals 方法，仅比较卡牌数字是否相同。
     * @param o 要比较的对象
     * @return 如果卡牌数字相同则返回 true，否则返回 false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return number == card.number;
    }

    /**
     * 重写 hashCode 方法，仅基于卡牌数字生成哈希码。
     * @return 卡牌数字的哈希码
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(number);
    }
}