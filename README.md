# Top Hog All - 多游戏平台

这是一个支持多种在线游戏的平台，目前支持"谁是猪头王"（牛头王）游戏，并具备通过配置热发布新游戏的能力。

## 项目组成

- **top_hog_server**: 基于 Java Spring Boot 的后端服务器
- **top_hog_web**: 基于 Vue 3 的 Web 前端
- **top_hog_miniprogram**: 微信小程序客户端

## 核心特性

- ✅ 支持多种游戏类型
- ✅ 配置驱动的游戏管理
- ✅ 热更新游戏配置（无需重启）
- ✅ WebSocket 实时通信
- ✅ 机器人玩家支持
- ✅ 托管模式
- ✅ 私密房间
- ✅ 快速匹配

## 文档导航

### 用户文档
- [项目使用指南](./GUIDE.md) - 开发、打包与发布指南

### 开发文档
- [多游戏支持架构](./MULTI_GAME_GUIDE.md) - 完整的多游戏架构设计与实现文档
- [快速开始指南](./QUICK_START.md) - 5分钟添加新游戏
- [API文档](./API_DOCUMENTATION.md) - 游戏配置管理API

### 配置文档
- [游戏配置说明](./top_hog_server/src/main/resources/game_configs/README.md) - 配置文件格式说明

## 快速开始

### 后端启动

```bash
cd top_hog_server
mvn spring-boot:run
```

访问 Swagger API 文档: http://localhost:8088/swagger-ui/index.html

### Web前端启动

```bash
cd top_hog_web
npm install
npm run dev
```

访问: http://localhost:5173

### 微信小程序

使用微信开发者工具打开 `top_hog_miniprogram` 目录。

## 添加新游戏

只需3步即可添加新游戏：

1. **定义游戏类型** - 在 `GameType.java` 中添加枚举
2. **实现游戏引擎** - 实现 `GameEngine` 接口
3. **配置游戏参数** - 创建配置文件或通过API配置

详细教程请参考 [快速开始指南](./QUICK_START.md)。

## 架构亮点

### 多游戏支持架构

```
游戏类型枚举 (GameType)
    ↓
游戏引擎接口 (GameEngine)
    ↓
游戏引擎工厂 (GameEngineFactory)
    ↓
具体游戏实现 (TopHogGameEngine, ...)
```

- 开放-封闭原则: 对扩展开放，对修改封闭
- 依赖倒置原则: 依赖抽象而非具体实现
- 配置驱动: 通过配置热更新游戏

### 配置热更新

无需重启服务器即可：
- 添加新游戏
- 修改游戏参数
- 启用/禁用游戏
- 更新游戏规则

## 技术栈

### 后端
- Java 17
- Spring Boot 3.x
- WebSocket
- MySQL
- MyBatis/JPA

### 前端
- Vue 3
- Vite
- WeChat Mini Program

## 开发团队

欢迎贡献代码和提出建议！

## 许可证

[许可证信息]
