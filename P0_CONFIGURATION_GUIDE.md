# P0级商业化功能配置指南

## 一、环境变量配置

### 1. Jasypt加密密钥

**Windows (PowerShell):**
```powershell
$env:JASYPT_PASSWORD="TopHogSecretKey2024"
```

**Linux/Mac:**
```bash
export JASYPT_PASSWORD="TopHogSecretKey2024"
```

**生产环境建议:**
使用更复杂的密钥,并通过Docker Secrets或Kubernetes Secrets管理。

---

## 二、生成加密配置值

### 1. 编译项目
```bash
cd top_hog_server
mvn clean compile
```

### 2. 运行加密工具
```bash
mvn exec:java -Dexec.mainClass="com.example.top_hog_server.util.JasyptEncryptorUtil"
```

### 3. 复制输出的加密值

工具会输出类似以下内容:
```
=== 加密结果 ===
数据库密码: ENC(xxxxxx)
微信AppSecret: ENC(yyyyyy)
邮箱密码: ENC(zzzzzz)
JWT密钥: ENC(wwwwww)
```

### 4. 更新application.properties

将明文替换为加密值:
```properties
# 数据库配置
spring.datasource.password=ENC(xxxxxx)

# 微信配置
wechat.app-secret=ENC(yyyyyy)

# 邮箱配置
spring.mail.password=ENC(zzzzzz)

# JWT配置
taskmanager.app.jwtSecret=ENC(wwwwww)
```

---

## 三、数据库初始化

### 1. 执行初始化脚本
```bash
mysql -u root -p top_hog_db < src/main/resources/db/init_commercialization.sql
```

### 2. 验证数据
```sql
-- 查看商品列表
SELECT * FROM product;

-- 查看表结构
SHOW TABLES;
```

---

## 四、微信支付配置

### 1. 申请商户号

访问: https://pay.weixin.qq.com/
- 注册商户号
- 完成资质认证
- 获取商户号(mchid)

### 2. 配置API密钥

在微信商户平台:
- 账户中心 → API安全 → 设置API密钥
- 下载API证书

### 3. 更新配置文件

```properties
# 微信支付配置
wechat.pay.mchid=你的商户号
wechat.pay.api-key=ENC(加密后的API密钥)
wechat.pay.cert-path=/path/to/apiclient_cert.p12
wechat.pay.notify-url=https://yourdomain.com/api/payment/notify
```

### 4. 测试环境

微信支付沙箱: https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=23_1

---

## 五、启动应用

### 1. 设置环境变量
```bash
export JASYPT_PASSWORD="TopHogSecretKey2024"
```

### 2. 启动服务
```bash
cd top_hog_server
mvn spring-boot:run
```

### 3. 验证启动
访问: http://localhost:8088/swagger-ui/index.html

检查以下接口:
- GET /api/products - 商品列表
- POST /api/payment/create - 创建订单

---

## 六、API测试

### 1. 获取商品列表
```bash
curl http://localhost:8088/api/products
```

### 2. 创建订单(需要登录)
```bash
curl -X POST http://localhost:8088/api/payment/create \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId": 1}'
```

### 3. 测试内容安全
```bash
# 修改昵称(包含敏感词会被拦截)
curl -X PUT http://localhost:8088/api/user \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nickname": "测试昵称"}'
```

---

## 七、生产部署检查清单

### 安全性
- [ ] 敏感配置已加密
- [ ] JASYPT_PASSWORD通过环境变量传入
- [ ] 数据库密码已加密
- [ ] 微信AppSecret已加密
- [ ] 启用HTTPS
- [ ] 配置防火墙规则

### 功能性
- [ ] 数据库表已创建
- [ ] 初始商品数据已插入
- [ ] 微信支付已配置
- [ ] 支付回调URL可公网访问
- [ ] 内容安全API已测试

### 监控
- [ ] 配置日志收集
- [ ] 配置性能监控
- [ ] 配置告警规则
- [ ] 配置支付异常监控

---

## 八、常见问题

### Q1: Jasypt解密失败
**A:** 检查JASYPT_PASSWORD环境变量是否正确设置

### Q2: 微信支付回调失败
**A:** 
1. 检查回调URL是否公网可访问
2. 检查签名验证逻辑
3. 查看微信商户平台的回调日志

### Q3: 内容安全检测失败
**A:** 
1. 检查微信AppSecret是否正确
2. 检查access_token是否过期
3. 查看ContentSecurityService日志

### Q4: 订单创建失败
**A:**
1. 检查商品是否存在
2. 检查用户是否已登录
3. 查看PaymentService日志

---

## 九、下一步工作

1. **对接真实微信支付API**
   - 实现签名算法
   - 调用统一下单接口
   - 实现支付回调签名验证

2. **完善测试**
   - 单元测试
   - 集成测试
   - 压力测试

3. **性能优化**
   - 添加Redis缓存
   - 优化数据库查询
   - 添加接口限流

4. **运营功能**
   - GM管理后台
   - 数据分析报表
   - 用户行为追踪
