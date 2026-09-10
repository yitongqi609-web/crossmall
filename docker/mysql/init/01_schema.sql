-- =============================================================
-- CrossMall · 潮汐全球购 数据库 Schema
-- 金额规范:全部以【分】为单位存储(BIGINT),基准币种 USD
-- 汇率规范:rate 表示 1 USD = rate 目标币
-- =============================================================
-- 初始化客户端会话必须是 utf8mb4,否则种子数据中文会被双重编码
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS crossmall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE crossmall;

-- 买家
CREATE TABLE IF NOT EXISTS `user` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `email`       VARCHAR(128) NOT NULL COMMENT '邮箱(登录账号)',
  `password`    VARCHAR(128) NOT NULL COMMENT '密码(BCrypt,{raw}前缀表示待初始化明文)',
  `nickname`    VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB COMMENT '买家表';

-- 收货地址(国际格式)
CREATE TABLE IF NOT EXISTS `address` (
  `id`             BIGINT NOT NULL AUTO_INCREMENT,
  `user_id`        BIGINT       NOT NULL,
  `receiver_name`  VARCHAR(64)  NOT NULL COMMENT '收件人',
  `phone`          VARCHAR(32)  NOT NULL COMMENT '联系电话(含国际区号)',
  `country_code`   VARCHAR(8)   NOT NULL COMMENT 'ISO2 国家码,如 US/GB/JP',
  `country_name`   VARCHAR(64)  NOT NULL COMMENT '国家名',
  `state_province` VARCHAR(64)  DEFAULT NULL COMMENT '州/省',
  `city`           VARCHAR(64)  NOT NULL COMMENT '城市',
  `street`         VARCHAR(255) NOT NULL COMMENT '街道门牌',
  `postcode`       VARCHAR(32)  NOT NULL COMMENT '邮编',
  `is_default`     TINYINT      NOT NULL DEFAULT 0 COMMENT '1默认地址',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB COMMENT '收货地址表';

-- 管理员
CREATE TABLE IF NOT EXISTS `admin_user` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT,
  `username`    VARCHAR(64)  NOT NULL,
  `password`    VARCHAR(128) NOT NULL COMMENT 'BCrypt,{raw}前缀表示待初始化明文',
  `nickname`    VARCHAR(64)  DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB COMMENT '管理员表';

-- 商品分类(双语)
CREATE TABLE IF NOT EXISTS `category` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(64) NOT NULL COMMENT '中文名',
  `name_en`     VARCHAR(64) NOT NULL COMMENT '英文名',
  `icon`        VARCHAR(16) DEFAULT NULL COMMENT '分类图标(emoji,离线可用)',
  `sort`        INT         NOT NULL DEFAULT 0,
  `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT '商品分类表';

-- SPU(双语标题/详情,关联 HS 编码用于关税)
CREATE TABLE IF NOT EXISTS `spu` (
  `id`             BIGINT NOT NULL AUTO_INCREMENT,
  `category_id`    BIGINT       NOT NULL,
  `title`          VARCHAR(255) NOT NULL COMMENT '中文标题',
  `title_en`       VARCHAR(255) NOT NULL COMMENT '英文标题',
  `description`    TEXT         COMMENT '中文详情',
  `description_en` TEXT         COMMENT '英文详情',
  `brand`          VARCHAR(64)  DEFAULT NULL,
  `hs_code`        VARCHAR(16)  NOT NULL COMMENT 'HS 编码(关税税则匹配)',
  `weight_grams`   INT          NOT NULL DEFAULT 500 COMMENT '单件计费重量(克)',
  `main_image`     VARCHAR(512) DEFAULT NULL COMMENT '主图URL(空则前端用分类图标占位)',
  `sales`          INT          NOT NULL DEFAULT 0 COMMENT '累计销量',
  `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_status_sales` (`status`, `sales`)
) ENGINE=InnoDB COMMENT '商品SPU表';

-- SKU(基准币 USD 分定价)
CREATE TABLE IF NOT EXISTS `sku` (
  `id`           BIGINT NOT NULL AUTO_INCREMENT,
  `spu_id`       BIGINT        NOT NULL,
  `attrs`        VARCHAR(1024) NOT NULL COMMENT '规格JSON:[{"k":"颜色","kEn":"Color","v":"黑","vEn":"Black"}]',
  `price_cents`  BIGINT        NOT NULL COMMENT '单价(USD分)',
  `stock`        INT           NOT NULL DEFAULT 0 COMMENT 'DB 库存(权威值)',
  `weight_grams` INT           NOT NULL DEFAULT 500 COMMENT '计费重量(克)',
  `status`       TINYINT       NOT NULL DEFAULT 1 COMMENT '1启用',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_spu` (`spu_id`)
) ENGINE=InnoDB COMMENT '商品SKU表';

-- 汇率表(1 USD = rate)
CREATE TABLE IF NOT EXISTS `fx_rate` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT,
  `currency`    VARCHAR(8)      NOT NULL COMMENT '目标币种',
  `currency_en` VARCHAR(32)     NOT NULL COMMENT '币种英文名',
  `symbol`      VARCHAR(8)      NOT NULL COMMENT '符号,如 $ € ¥ £',
  `rate`        DECIMAL(12, 6)  NOT NULL COMMENT '1 USD = rate',
  `source`      VARCHAR(16)     NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL手动/AUTO定时拉取',
  `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_currency` (`currency`)
) ENGINE=InnoDB COMMENT '汇率表';

-- 关税税则(国家 × HS 编码,hs_code='*' 为该国默认税率)
CREATE TABLE IF NOT EXISTS `hs_tax` (
  `id`                  BIGINT NOT NULL AUTO_INCREMENT,
  `country_code`        VARCHAR(8)     NOT NULL COMMENT '目的国 ISO2',
  `country_name`        VARCHAR(64)    NOT NULL,
  `hs_code`             VARCHAR(16)    NOT NULL COMMENT 'HS编码,* 表示该国通用',
  `hs_name`             VARCHAR(64)    NOT NULL COMMENT '品类名称',
  `tax_rate`            DECIMAL(6, 4)  NOT NULL COMMENT '关税税率,0.0800=8%',
  `de_minimis_cents`    BIGINT         NOT NULL DEFAULT 0 COMMENT '免税额度(USD分),商品额低于此值免关税,0=无',
  `create_time`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_country_hs` (`country_code`, `hs_code`)
) ENGINE=InnoDB COMMENT '关税税则表';

-- 物流线路
CREATE TABLE IF NOT EXISTS `logistics_line` (
  `id`                     BIGINT NOT NULL AUTO_INCREMENT,
  `name`                   VARCHAR(64)  NOT NULL COMMENT '线路中文名',
  `name_en`                VARCHAR(64)  NOT NULL COMMENT '线路英文名',
  `carrier`                VARCHAR(64)  NOT NULL COMMENT '承运商,如 4PX/云途/DHL',
  `supported_countries`    VARCHAR(512) NOT NULL COMMENT '支持目的国JSON:["*"]或["US","CA"]',
  `first_weight_grams`     INT          NOT NULL COMMENT '首重(克)',
  `first_fee_cents`        BIGINT       NOT NULL COMMENT '首重费用(USD分)',
  `continue_weight_grams`  INT          NOT NULL COMMENT '续重计费单位(克)',
  `continue_fee_cents`     BIGINT       NOT NULL COMMENT '每续重单位费用(USD分)',
  `eta_min_days`           INT          NOT NULL COMMENT '预计时效下限(天)',
  `eta_max_days`           INT          NOT NULL COMMENT '预计时效上限(天)',
  `status`                 TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用',
  `create_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT '物流线路表';

-- 订单表(核心:汇率快照 + 三段金额)
CREATE TABLE IF NOT EXISTS `orders` (
  `id`                  BIGINT NOT NULL AUTO_INCREMENT,
  `order_no`            VARCHAR(32)    NOT NULL COMMENT '业务订单号',
  `user_id`             BIGINT         NOT NULL,
  `status`              VARCHAR(24)    NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT 'PENDING_PAYMENT/PAID/STOCKED/DECLARED/IN_TRANSIT/CUSTOMS_CLEARANCE/DELIVERING/COMPLETED/CANCELLED/REFUNDED',
  `address_snapshot`    VARCHAR(1024)  NOT NULL COMMENT '收货地址快照JSON',
  `logistics_line_id`   BIGINT         NOT NULL,
  `line_name`           VARCHAR(64)    NOT NULL COMMENT '线路名快照',
  `currency`            VARCHAR(8)     NOT NULL COMMENT '结算币种',
  `fx_rate`             DECIMAL(12, 6) NOT NULL COMMENT '汇率快照(1 USD = fx)',
  `goods_fee_cents`     BIGINT         NOT NULL COMMENT '商品总额(USD分)',
  `shipping_fee_cents`  BIGINT         NOT NULL COMMENT '运费(USD分)',
  `tax_fee_cents`       BIGINT         NOT NULL COMMENT '关税(USD分)',
  `total_fee_cents`     BIGINT         NOT NULL COMMENT '订单总额(USD分)=商品+运费+关税',
  `total_local_cents`   BIGINT         NOT NULL COMMENT '本币应付总额(分),=total*fx快照',
  `pay_timeout_at`      DATETIME       NOT NULL COMMENT '支付截止时间(超时关单)',
  `paid_at`             DATETIME       DEFAULT NULL,
  `completed_at`        DATETIME       DEFAULT NULL,
  `cancel_reason`       VARCHAR(255)   DEFAULT NULL,
  `create_time`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_status_timeout` (`status`, `pay_timeout_at`)
) ENGINE=InnoDB COMMENT '订单表';

CREATE TABLE IF NOT EXISTS `order_item` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT,
  `order_id`      BIGINT        NOT NULL,
  `order_no`      VARCHAR(32)   NOT NULL,
  `spu_id`        BIGINT        NOT NULL,
  `sku_id`        BIGINT        NOT NULL,
  `spu_title`     VARCHAR(255)  NOT NULL COMMENT '标题快照(中文)',
  `spu_title_en`  VARCHAR(255)  NOT NULL COMMENT '标题快照(英文)',
  `sku_attrs`     VARCHAR(1024) NOT NULL COMMENT '规格快照JSON',
  `image`         VARCHAR(512)  DEFAULT NULL,
  `price_cents`   BIGINT        NOT NULL COMMENT '成交单价(USD分)快照',
  `quantity`      INT           NOT NULL,
  `subtotal_cents` BIGINT       NOT NULL COMMENT '小计(USD分)',
  `weight_grams`  INT           NOT NULL COMMENT '单件计费重量快照',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB COMMENT '订单明细表';

-- 支付流水(支付幂等核心表:txn_no 唯一索引)
CREATE TABLE IF NOT EXISTS `payment_transaction` (
  `id`           BIGINT NOT NULL AUTO_INCREMENT,
  `txn_no`       VARCHAR(40)   NOT NULL COMMENT '支付流水号(全局唯一)',
  `order_no`     VARCHAR(32)   NOT NULL,
  `channel`      VARCHAR(16)   NOT NULL COMMENT 'PAYPAL/CARD',
  `currency`     VARCHAR(8)    NOT NULL COMMENT '支付币种',
  `fx_rate`      DECIMAL(12, 6) NOT NULL COMMENT '支付时汇率(=订单快照)',
  `amount_local` BIGINT        NOT NULL COMMENT '本币支付金额(分)',
  `amount_usd`   BIGINT        NOT NULL COMMENT '折算美元(分)',
  `status`       VARCHAR(16)   NOT NULL DEFAULT 'INIT' COMMENT 'INIT/SUCCESS/REFUNDED/VOID',
  `callback_payload` VARCHAR(1024) DEFAULT NULL COMMENT '最近一次回调报文',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `paid_at`      DATETIME      DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_txn_no` (`txn_no`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB COMMENT '支付流水表';

-- 履约轨迹(每有一次状态流转就有一条)
CREATE TABLE IF NOT EXISTS `order_track` (
  `id`             BIGINT NOT NULL AUTO_INCREMENT,
  `order_no`       VARCHAR(32)  NOT NULL,
  `status`         VARCHAR(24)  NOT NULL COMMENT '轨迹节点状态',
  `location`       VARCHAR(128) DEFAULT NULL COMMENT '节点地点,如 Shenzhen CN',
  `description`    VARCHAR(255) NOT NULL COMMENT '节点描述(中文)',
  `description_en` VARCHAR(255) NOT NULL COMMENT '节点描述(英文)',
  `occurred_at`    DATETIME     NOT NULL COMMENT '节点发生时间',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB COMMENT '订单履约轨迹表';
