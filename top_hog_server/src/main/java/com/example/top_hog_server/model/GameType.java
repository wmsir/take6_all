package com.example.top_hog_server.model;

/**
 * 游戏类型枚举
 * 定义系统支持的所有游戏类型
 */
public enum GameType {
    TOP_HOG("top_hog", "谁是猪头王", "经典牛头王游戏，避免收集牛头卡牌"),
    // 未来可以添加更多游戏类型
    // EXAMPLE_GAME("example_game", "示例游戏", "示例游戏描述"),
    ;

    private final String code;
    private final String displayName;
    private final String description;

    GameType(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取游戏类型
     */
    public static GameType fromCode(String code) {
        for (GameType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return TOP_HOG; // 默认返回猪头王
    }
}
