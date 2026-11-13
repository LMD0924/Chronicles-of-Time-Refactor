-- V7 免登录只读分享链接（D3 用户已确认形态）
-- 1) share_link：随机 token 即访问凭证，支持有效期、撤销、访问计数。
-- 2) 公开只读端点 GET /api/v1/timeline/share/{token}（已在鉴权白名单前缀内），
--    生成/列表/撤销/续期走 /api/v1/planning/share-links（需登录）。
-- 3) 时间统一存 VARCHAR(yyyy-MM-dd HH:mm:ss)，便于 MySQL 8 / H2 双兼容与字典序比较。
-- 注意：Flyway 已执行过的迁移不可回改内容（checksum），后续调整请新增 V8。

CREATE TABLE IF NOT EXISTS share_link (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(64) NOT NULL,
    title VARCHAR(255),
    expires_at VARCHAR(32),
    revoked TINYINT(1) DEFAULT 0,
    view_count INT DEFAULT 0,
    last_viewed_at VARCHAR(32),
    created_at VARCHAR(32)
);

CREATE UNIQUE INDEX idx_share_token ON share_link(token);
CREATE INDEX idx_share_user ON share_link(user_id);
