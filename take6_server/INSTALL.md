# 项目安装与启动指南

本指南将指导您如何搭建、安装和启动 Take6 Server 项目。

## 环境要求

在开始之前，请确保您的开发环境满足以下要求：

*   **Java**: JDK 17 (或更高版本，推荐 JDK 17)
*   **Maven**: 3.6 或更高版本
*   **MySQL**: 5.7 或 8.0
*   **Git**: 用于克隆代码

## 1. 获取代码

如果您是从代码仓库获取的代码，请先克隆到本地：

```bash
git clone <repository-url>
cd take6server
```

## 2. 数据库配置

项目默认使用 MySQL 数据库。在启动应用之前，您需要配置数据库连接信息。

1.  找到配置文件：`src/main/resources/application.properties`
2.  修改以下配置项以匹配您的 MySQL 环境：

```properties
# 数据库地址、端口和数据库名称
spring.datasource.url=jdbc:mysql://localhost:3306/take6_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
# 数据库用户名
spring.datasource.username=your_username
# 数据库密码
spring.datasource.password=your_password
```

**注意**：请确保数据库 `take6_db` 已经创建。如果未创建，请登录 MySQL 执行：

```sql
CREATE DATABASE take6_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 3. 邮件服务器配置 (可选)

项目使用了邮件功能（如注册验证码等），如果需要完整功能，请配置邮件服务器：

```properties
spring.mail.host=smtp.qq.com
spring.mail.port=587
spring.mail.username=your_email@example.com
spring.mail.from=your_email@example.com
spring.mail.password=your_email_password_or_auth_code
```

## 4. 编译与打包

在项目根目录下，运行以下 Maven 命令来清理并打包项目：

```bash
mvn clean package
```

如果想要跳过测试（例如在没有数据库环境的情况下构建），可以使用：

```bash
mvn clean package -DskipTests
```

构建成功后，将在 `target` 目录下生成一个 JAR 文件，例如 `take6server-0.0.1-SNAPSHOT.jar`。

## 5. 启动应用

使用 `java -jar` 命令启动应用：

```bash
java -jar target/take6server-0.0.1-SNAPSHOT.jar
```

或者直接使用 Maven 启动：

```bash
mvn spring-boot:run
```

启动成功后，您应该能在控制台看到 Spring Boot 的启动日志。

## 6. 验证与 API 文档

项目集成了 Swagger (OpenAPI) 文档。应用启动后，您可以访问以下地址查看 API 文档：

*   **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
*   **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## 常见问题

*   **数据库连接失败**：请检查 `application.properties` 中的 URL、用户名和密码是否正确，以及 MySQL 服务是否已启动。
*   **端口被占用**：默认使用 8080 端口。如果被占用，可以在 `application.properties` 中修改 `server.port`。
