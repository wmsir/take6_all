package com.example.take6server.model;

/**
 * 定义游戏房间可能存在的各种状态。
 */
public enum GameState {
    WAITING,         // 等待玩家加入
    DEALING,         // 正在发牌 (通常是新游戏或新一轮开始的短暂状态)
    PLAYING,         // 玩家出牌阶段
    PROCESSING_TURN, // 服务器处理本轮所有玩家的出牌
    WAITING_FOR_PLAYER_CHOICE, // 等待某个玩家选择要拿走哪一行牌
    ROUND_OVER,      // 一轮（10次出牌）结束，准备开始新一轮或结束游戏
    GAME_OVER        // 整个游戏（多轮）结束
}