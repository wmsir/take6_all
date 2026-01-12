# 谁是猪头王 - 多游戏平台

> 一个支持多种在线卡牌游戏的平台,目前支持"谁是猪头王"(牛头王)游戏,并具备通过配置热发布新游戏的能力。

## 📋 项目简介

**谁是猪头王**是一款基于Spring Boot + Vue 3 + 微信小程序的多人在线卡牌游戏平台。

### 核心特性

- ✅ **多游戏支持** - 可插拔的游戏引擎架构,轻松添加新游戏
- ✅ **实时对战** - WebSocket实时通信,流畅的游戏体验
- ✅ **多端支持** - Web端 + 微信小程序
- ✅ **智能托管** - 机器人玩家和托管模式
- ✅ **商业化就绪** - 支付系统、VIP会员、虚拟货币
- ✅ **内容安全** - 微信内容安全API对接

## 🏗️ 项目结构

```
top_hog_all/
├── top_hog_server/        # 后端服务 (Spring Boot)
├── top_hog_web/           # Web前端 (Vue 3)
├── top_hog_miniprogram/   # 微信小程序
└── docs/                  # 项目文档
```

## 🚀 快速开始

### 环境要求

- **后端**: Java 17+, Maven 3.6+, MySQL 8.0+
- **Web前端**: Node.js 16+, npm 8+
- **小程序**: 微信开发者工具

### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE top_hog_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 执行初始化脚本
mysql -u root -p top_hog_db < top_hog_server/src/main/resources/db/init.sql
```

### 2. 启动后端服务

```bash
cd top_hog_server
mvn spring-boot:run
```

访问 Swagger API 文档: http://localhost:8088/swagger-ui/index.html

### 3. 启动Web前端

```bash
cd top_hog_web
npm install
npm run dev
```

访问: http://localhost:5173

### 4. 启动微信小程序

使用微信开发者工具打开 `top_hog_miniprogram` 目录。

## 📚 文档导航

### 用户文档
- [快速开始指南](./docs/快速开始.md) - 5分钟添加新游戏
- [部署指南](./docs/部署指南.md) - 开发、打包与发布

### 开发文档
- [多游戏架构](./docs/多游戏架构.md) - 完整的多游戏架构设计
- [游戏配置说明](./docs/游戏配置.md) - 配置文件格式说明
- [API文档](./docs/API文档.md) - 游戏配置管理API

### 商业化文档
- [商业化计划](./docs/商业化计划.md) - 商业化功能规划和实施进度
- [P0功能配置指南](./docs/P0功能配置指南.md) - 支付、内容安全等核心功能配置

## 🎮 支持的游戏

### 谁是猪头王 (Top Hog)
经典的牛头王卡牌游戏,支持2-6人对战。

**游戏规则:**
- 每轮选择一张手牌
- 牌按数字大小排序后依次放入4行
- 第6张牌需要收走整行
- 收到的牛头数最多的玩家成为"猪头王"

## 💰 商业化功能

### 已实现功能
- ✅ **支付系统** - 微信支付对接
- ✅ **虚拟货币** - 金币、钻石系统
- ✅ **VIP会员** - 月卡、季卡、年卡
- ✅ **商城系统** - 商品管理、订单管理
- ✅ **内容安全** - 文本、图片安全检测

### 计划中功能
- ⏳ 广告变现 - 激励视频、Banner广告
- ⏳ 排行榜系统 - 全服排行、好友排行
- ⏳ 成就系统 - 成就徽章、奖励
- ⏳ 社交功能 - 好友系统、战队系统

详见 [商业化计划](./docs/商业化计划.md)

## 🛠️ 技术栈

### 后端
- **框架**: Spring Boot 3.x
- **数据库**: MySQL 8.0
- **ORM**: JPA/Hibernate
- **实时通信**: WebSocket
- **安全**: Spring Security + JWT
- **文档**: Swagger/OpenAPI

### Web前端
- **框架**: Vue 3
- **构建工具**: Vite
- **HTTP客户端**: Axios
- **路由**: Vue Router

### 微信小程序
- **框架**: 原生小程序
- **UI**: 自定义组件
- **状态管理**: 本地存储

## 📊 架构亮点

### 多游戏支持架构

```
GameType (游戏类型枚举)
    ↓
GameEngine (游戏引擎接口)
    ↓
GameEngineFactory (游戏引擎工厂)
    ↓
具体游戏实现 (TopHogGameEngine, ...)
```

**设计原则:**
- 开放-封闭原则: 对扩展开放,对修改封闭
- 依赖倒置原则: 依赖抽象而非具体实现
- 配置驱动: 通过配置热更新游戏

### 配置热更新

无需重启服务器即可:
- 添加新游戏
- 修改游戏参数
- 启用/禁用游戏
- 更新游戏规则

## 🔐 安全特性

- **敏感信息加密** - Jasypt加密配置
- **内容安全** - 微信内容安全API
- **JWT认证** - 无状态身份验证
- **HTTPS** - 生产环境强制HTTPS
- **SQL注入防护** - JPA参数化查询

## 📈 性能优化

- **数据库索引** - 关键字段索引优化
- **连接池** - HikariCP高性能连接池
- **异步处理** - WebSocket异步消息
- **缓存策略** - 游戏配置缓存

## 🤝 贡献指南

欢迎贡献代码和提出建议!

### 添加新游戏

只需3步:
1. 在 `GameType.java` 中添加游戏类型枚举
2. 实现 `GameEngine` 接口
3. 创建游戏配置文件

详见 [快速开始指南](./docs/快速开始.md)

## 📝 更新日志

### v1.0.0 (2026-01-12)
- ✅ 完成P0级商业化功能
- ✅ 支付系统、VIP会员、虚拟货币
- ✅ 内容安全API对接
- ✅ 微信小程序UI优化

### v0.9.0
- ✅ 多游戏架构重构
- ✅ 配置热更新
- ✅ WebSocket实时通信

## 📄 许可证

MIT License

## 👥 开发团队

欢迎加入我们,一起打造更好的游戏平台!

## 📞 联系方式

- 项目地址: [GitHub](https://github.com/your-repo/top_hog_all)
- 问题反馈: [Issues](https://github.com/your-repo/top_hog_all/issues)

---

**Made with ❤️ by Top Hog Team**
