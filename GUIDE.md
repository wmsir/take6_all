# 谁是猪头王 (Top Hog) 项目使用、打包与发布指南

本指南详细说明了“谁是猪头王”项目的各个组成部分（后端、Web 前端、微信小程序）的开发、构建和部署流程。

## 目录

1. [项目概览](#1-项目概览)
2. [环境准备](#2-环境准备)
3. [后端服务 (top_hog_server)](#3-后端服务-top_hog_server)
   - [配置](#31-配置)
   - [本地开发运行](#32-本地开发运行)
   - [打包构建](#33-打包构建)
   - [部署](#34-部署)
4. [Web 前端 (top_hog_web)](#4-web-前端-top_hog_web)
   - [安装依赖](#41-安装依赖)
   - [本地开发运行](#42-本地开发运行)
   - [打包构建](#43-打包构建)
   - [部署](#44-部署)
5. [微信小程序 (top_hog_miniprogram)](#5-微信小程序-top_hog_miniprogram)
   - [导入与配置](#51-导入与配置)
   - [调试与预览](#52-调试与预览)
   - [发布](#53-发布)

---

## 1. 项目概览

本项目由三个主要部分组成：

*   **top_hog_server**: 基于 Java Spring Boot 的后端服务器，提供 REST API 和 WebSocket 游戏服务。
*   **top_hog_web**: 基于 Vue 3 + Vite 的 Web 管理端/游戏前端。
*   **top_hog_miniprogram**: 微信小程序客户端。

## 2. 环境准备

在开始之前，请确保您的开发环境安装了以下工具：

*   **Java**: JDK 17+ (后端)
*   **Maven**: 3.6+ (后端构建)
*   **Node.js**: 16+ (Web 前端)
*   **MySQL**: 5.7 或 8.0 (数据库)
*   **微信开发者工具**: 最新版 (小程序)
*   **Git**: 版本控制

---

## 3. 后端服务 (top_hog_server)

### 3.1 配置

后端的主要配置文件位于 `top_hog_server/src/main/resources/application.properties`。在运行或打包之前，请根据实际环境修改配置：

*   **数据库配置**:
    ```properties
    spring.datasource.url=jdbc:mysql://localhost:3306/top_hog_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    spring.datasource.username=your_username
    spring.datasource.password=your_password
    ```
    *注意*: 请确保 MySQL 中已创建名为 `top_hog_db` 的数据库。

*   **服务端口**:
    ```properties
    server.port=8088
    ```
    默认为 `8088`，Web 端和小程序端默认连接此端口。

*   **邮件服务** (用于注册验证码等):
    修改 `spring.mail.*` 相关配置。

*   **JWT 配置**:
    建议修改 `taskmanager.app.jwtSecret` 为一个安全的随机字符串。

*   **阿里云 OSS** (可选):
    如果使用对象存储，请配置 `aliyun.oss.*`。

### 3.2 本地开发运行

在 `top_hog_server` 目录下：

1.  **使用 Maven 插件运行**:
    ```bash
    mvn spring-boot:run
    ```

2.  **或者直接运行 Main 类**:
    在 IDE (IntelliJ IDEA, Eclipse) 中找到主类并运行。

启动成功后，Swagger API 文档地址: `http://localhost:8088/swagger-ui/index.html`

### 3.3 打包构建

生成可执行的 JAR 包：

```bash
cd top_hog_server
mvn clean package
```

*   如果需要跳过测试：`mvn clean package -DskipTests`
*   构建产物位于 `target/top_hog_server-0.0.1-SNAPSHOT.jar`。

### 3.4 部署

将生成的 JAR 包上传至服务器，运行：

```bash
java -jar top_hog_server-0.0.1-SNAPSHOT.jar
```

**建议使用 Systemd 管理服务 (Linux)**:

创建文件 `/etc/systemd/system/tophog.service`:

```ini
[Unit]
Description=Top Hog Server
After=syslog.target network.target

[Service]
User=root
ExecStart=/usr/bin/java -jar /path/to/your/top_hog_server-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=always

[Install]
WantedBy=multi-user.target
```

启动服务：
```bash
systemctl enable tophog
systemctl start tophog
```

---

## 4. Web 前端 (top_hog_web)

### 4.1 安装依赖

在 `top_hog_web` 目录下：

```bash
npm install
```

### 4.2 本地开发运行

```bash
npm run dev
```
默认运行在 `http://localhost:5173` (或其他可用端口)。
*   **代理配置**: `vite.config.js` 中已配置 `/api` 和 `/ws-game` 代理转发至 `http://localhost:8088`。确保后端服务已启动。

### 4.3 打包构建

构建生产环境代码：

```bash
npm run build
```
构建产物将输出到 `dist` 目录。

### 4.4 部署

将 `dist` 目录下的所有文件上传至 Web 服务器 (如 Nginx, Apache)。

**Nginx 配置示例**:

```nginx
server {
    listen 80;
    server_name your_domain.com;

    root /path/to/dist;
    index index.html;

    # 处理 Vue Router 的 History 模式
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 代理 API 请求到后端
    location /api {
        proxy_pass http://localhost:8088;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 代理 WebSocket 请求
    location /ws-game {
        proxy_pass http://localhost:8088;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
    }
}
```

---

## 5. 微信小程序 (top_hog_miniprogram)

### 5.1 导入与配置

1.  打开 **微信开发者工具**。
2.  选择“导入项目”，目录选择 `top_hog_miniprogram`。
3.  填写您的 AppID (或使用测试号)。

**服务器地址配置**:
打开 `app.js`，修改 `globalData` 中的地址以匹配您的部署环境：

```javascript
globalData: {
  // 本地开发
  baseUrl: 'http://localhost:8088',
  wsUrl: 'ws://localhost:8088/ws-game',

  // 生产环境示例
  // baseUrl: 'https://your_domain.com',
  // wsUrl: 'wss://your_domain.com/ws-game',
  // ...
}
```

*注意*: 微信小程序生产环境要求使用 HTTPS 和 WSS 协议，且域名需在微信公众平台后台配置为合法域名。

### 5.2 调试与预览

*   在开发者工具中点击“编译”即可在模拟器中预览。
*   点击“真机调试”可生成二维码，使用手机微信扫描进行调试。

### 5.3 发布

1.  在开发者工具中点击“上传”，填写版本号和备注。
2.  登录 [微信公众平台](https://mp.weixin.qq.com)。
3.  在“版本管理”中找到上传的开发版本。
4.  提交审核。
5.  审核通过后，提交发布。
