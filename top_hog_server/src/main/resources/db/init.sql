-- ============================================
-- 谁是猪头王 - 完整数据库初始化脚本
-- ============================================
-- 说明: 此脚本包含所有必需的表结构和初始数据
-- 用途: 项目首次部署时执行
-- ============================================

-- 1. 用户表 (Users)
-- 已由JPA自动创建,这里添加商业化相关字段
ALTER TABLE Users ADD COLUMN IF NOT EXISTS vip_expire_time DATETIME NULL COMMENT 'VIP过期时间';

-- 2. 商品表 (product)
CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    name VARCHAR(100) NOT NULL COMMENT '商品名称',
    type VARCHAR(20) NOT NULL COMMENT '商品类型: VIP, COINS, DIAMONDS',
    price DECIMAL(10,2) NOT NULL COMMENT '售价',
    original_price DECIMAL(10,2) COMMENT '原价',
    coins BIGINT DEFAULT 0 COMMENT '金币数量',
    diamonds BIGINT DEFAULT 0 COMMENT '钻石数量',
    vip_days INT DEFAULT 0 COMMENT 'VIP天数',
    description TEXT COMMENT '商品描述',
    icon_url VARCHAR(255) COMMENT '商品图标URL',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, INACTIVE',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_type (type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 3. 订单表 (orders)
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no VARCHAR(32) UNIQUE NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(100) COMMENT '商品名称快照',
    amount DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING, PAID, FAILED, REFUNDED',
    payment_method VARCHAR(20) DEFAULT 'WECHAT' COMMENT '支付方式',
    transaction_id VARCHAR(64) COMMENT '微信交易ID',
    paid_at TIMESTAMP NULL COMMENT '支付时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 4. 用户余额表 (user_balance)
CREATE TABLE IF NOT EXISTS user_balance (
    user_id BIGINT PRIMARY KEY COMMENT '用户ID',
    coins BIGINT DEFAULT 0 COMMENT '金币余额',
    diamonds BIGINT DEFAULT 0 COMMENT '钻石余额',
    total_recharge DECIMAL(10,2) DEFAULT 0 COMMENT '累计充值金额',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户余额表';

-- 5. 交易记录表 (transaction_log)
CREATE TABLE IF NOT EXISTS transaction_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type VARCHAR(20) NOT NULL COMMENT '交易类型: RECHARGE, CONSUME, REWARD, REFUND',
    currency VARCHAR(20) NOT NULL COMMENT '货币类型: COINS, DIAMONDS',
    amount BIGINT NOT NULL COMMENT '变动数量',
    balance_before BIGINT NOT NULL COMMENT '变动前余额',
    balance_after BIGINT NOT NULL COMMENT '变动后余额',
    reason VARCHAR(100) COMMENT '交易原因',
    order_no VARCHAR(32) COMMENT '关联订单号',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易记录表';

-- ============================================
-- 初始数据
-- ============================================

-- 插入初始商品数据
INSERT INTO product (name, type, price, original_price, coins, diamonds, vip_days, description, status, sort_order) VALUES
-- VIP商品
('月度VIP', 'VIP', 30.00, 38.00, 0, 0, 30, 'VIP会员月卡,享受专属特权:免广告、专属头像框、每日金币翻倍', 'ACTIVE', 1),
('季度VIP', 'VIP', 78.00, 98.00, 0, 0, 90, 'VIP会员季卡,享受专属特权,更优惠', 'ACTIVE', 2),
('年度VIP', 'VIP', 268.00, 368.00, 0, 0, 365, 'VIP会员年卡,享受专属特权,超值优惠', 'ACTIVE', 3),

-- 金币商品
('金币包(小)', 'COINS', 6.00, NULL, 600, 0, 0, '600金币', 'ACTIVE', 4),
('金币包(中)', 'COINS', 30.00, NULL, 3300, 0, 0, '3000金币,赠送300金币', 'ACTIVE', 5),
('金币包(大)', 'COINS', 98.00, NULL, 12000, 0, 0, '10000金币,赠送2000金币', 'ACTIVE', 6),

-- 钻石商品
('钻石包(小)', 'DIAMONDS', 12.00, NULL, 0, 100, 0, '100钻石', 'ACTIVE', 7),
('钻石包(中)', 'DIAMONDS', 50.00, NULL, 0, 550, 0, '500钻石,赠送50钻石', 'ACTIVE', 8),
('钻石包(大)', 'DIAMONDS', 198.00, NULL, 0, 2400, 0, '2000钻石,赠送400钻石', 'ACTIVE', 9)

ON DUPLICATE KEY UPDATE 
    name = VALUES(name),
    price = VALUES(price),
    description = VALUES(description);

-- ============================================
-- 完成提示
-- ============================================
SELECT '数据库初始化完成! 已创建5个表,插入9个商品' AS message;
