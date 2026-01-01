# 游戏配置文件说明

本目录包含各个游戏的配置文件，用于定义游戏的基本属性和特定参数。

## 文件列表

- `game-config-schema.json` - 配置文件的JSON Schema定义
- `top_hog.json` - 谁是猪头王游戏配置
- `example_game.json` - 示例游戏配置（模板）

## 配置文件格式

每个游戏配置文件应遵循 `game-config-schema.json` 中定义的格式。

### 必需字段

- `gameTypeCode`: 游戏类型唯一标识（必须与代码中GameType枚举一致）
- `displayName`: 游戏显示名称
- `minPlayers`: 最小玩家数
- `maxPlayers`: 最大玩家数

### 可选字段

- `description`: 游戏简短描述
- `enabled`: 是否启用（默认true）
- `iconUrl`: 游戏图标URL
- `rulesDescription`: 详细游戏规则说明
- `displayOrder`: 显示顺序（数字越小越靠前）
- `gameSpecificConfig`: 游戏特定配置（对象格式）

## 如何添加新游戏配置

1. 复制 `example_game.json` 作为模板
2. 修改文件名为你的游戏类型代码（如 `my_game.json`）
3. 填写所有必需字段
4. 根据游戏需求自定义 `gameSpecificConfig` 部分
5. 将 `enabled` 设置为 `true` 以启用游戏

## 配置加载方式

配置可以通过以下两种方式加载：

### 方式1：数据库初始化

在 `GameConfigurationService.initializeDefaultConfigurations()` 方法中添加代码，服务启动时自动创建配置。

### 方式2：REST API

通过API接口动态创建或更新配置：

```bash
POST /api/game-config
Content-Type: application/json

{配置内容}
```

## 配置验证

配置文件应符合 `game-config-schema.json` 定义的格式。可以使用JSON Schema验证工具进行验证。

在线验证工具推荐：
- https://www.jsonschemavalidator.net/

## 热更新

修改配置后，可通过以下方式热更新：

```bash
# 更新配置
PUT /api/game-config/{gameTypeCode}

# 重新加载
POST /api/game-config/reload
```

## 注意事项

1. `gameTypeCode` 必须与后端 `GameType` 枚举中的代码完全一致
2. `gameSpecificConfig` 中的内容根据具体游戏引擎的需求自定义
3. 修改配置后建议重新验证JSON格式是否正确
4. 生产环境建议通过API进行配置管理，避免直接修改文件

## 示例

参考 `top_hog.json` 了解完整配置示例。
