-- 文档商店 MySQL 数据库初始化脚本
-- 适用：MySQL 8.0+
-- 说明：表结构与 JPA 实体一致（默认驼峰转下划线命名）。
--       文档数据由服务启动时从 res.csv 导入，无需在此插入初始数据。
-- 使用：mysql -u root -p < server/db/mysql-schema.sql
--       建议将 spring.jpa.hibernate.ddl-auto 设为 validate 或 none。

CREATE DATABASE IF NOT EXISTS document_store
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE document_store;

-- 微信登录用户
CREATE TABLE IF NOT EXISTS app_users (
  id                    BIGINT       NOT NULL AUTO_INCREMENT,
  open_id               VARCHAR(64)  NOT NULL COMMENT '微信 openid',
  union_id              VARCHAR(64)           COMMENT '微信 unionid',
  nickname              VARCHAR(128)          COMMENT '用户昵称',
  avatar_url            VARCHAR(1024)         COMMENT '头像地址',
  encrypted_session_key VARCHAR(1024) NOT NULL COMMENT 'AES-GCM 加密后的 session_key',
  created_at            DATETIME(6)  NOT NULL,
  updated_at            DATETIME(6)  NOT NULL,
  version               BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (id),
  UNIQUE KEY uk_app_users_open_id (open_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 文档目录，数据来自 res.csv
CREATE TABLE IF NOT EXISTS document_assets (
  id            VARCHAR(64)  NOT NULL COMMENT 'CSV 序号',
  sort_order    INT          NOT NULL COMMENT '排序号',
  note          VARCHAR(512)          COMMENT 'CSV 备注',
  title         VARCHAR(512)  NOT NULL COMMENT '文档名称',
  image_count   INT          NOT NULL COMMENT '包含图片数量',
  price_fen     INT                   COMMENT '价格，单位分；为空时用服务端兜底价',
  download_link VARCHAR(2048) NOT NULL COMMENT '网盘链接，不下发给列表接口',
  PRIMARY KEY (id),
  KEY idx_document_assets_sort_order (sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 虚拟支付订单
CREATE TABLE IF NOT EXISTS virtual_orders (
  id                BIGINT      NOT NULL AUTO_INCREMENT,
  out_trade_no      VARCHAR(64) NOT NULL COMMENT '商户订单号',
  user_id           BIGINT      NOT NULL COMMENT 'app_users.id',
  document_id       VARCHAR(64) NOT NULL COMMENT 'document_assets.id',
  amount_fen        INT         NOT NULL COMMENT '订单金额，单位分',
  status            VARCHAR(24) NOT NULL COMMENT 'PENDING/PAID/REFUNDED/FAILED',
  platform_order_no VARCHAR(128)         COMMENT '微信侧订单号',
  created_at        DATETIME(6) NOT NULL,
  paid_at           DATETIME(6)          COMMENT '回调确认支付时间',
  version           BIGINT      NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (id),
  UNIQUE KEY uk_virtual_orders_out_trade_no (out_trade_no),
  KEY idx_virtual_orders_user_document_status (user_id, document_id, status),
  KEY idx_virtual_orders_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 限时免费下载记录，唯一约束保证每人每天一次
CREATE TABLE IF NOT EXISTS free_download_usages (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  user_id     BIGINT      NOT NULL COMMENT 'app_users.id',
  document_id VARCHAR(64) NOT NULL COMMENT 'document_assets.id',
  usage_date  DATE        NOT NULL COMMENT '统计日期，按 app.free-download.time-zone 计算',
  created_at  DATETIME(6) NOT NULL,
  version     BIGINT      NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (id),
  UNIQUE KEY uk_free_download_user_date (user_id, usage_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 看广告免费下载记录，唯一约束保证每人每天一次
CREATE TABLE IF NOT EXISTS ad_download_usages (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  user_id     BIGINT      NOT NULL COMMENT 'app_users.id',
  document_id VARCHAR(64) NOT NULL COMMENT 'document_assets.id',
  usage_date  DATE        NOT NULL COMMENT '统计日期，按 app.ad.time-zone 计算',
  created_at  DATETIME(6) NOT NULL,
  version     BIGINT      NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ad_download_user_date (user_id, usage_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
