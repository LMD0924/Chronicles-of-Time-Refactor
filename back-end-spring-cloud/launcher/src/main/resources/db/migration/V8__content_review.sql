-- V8 内容审核字段（D4：草稿 → 管理员审核 → 发布 / 驳回）
-- 1) review_note：驳回理由，作者在「我的投稿」中可见，改稿后可重新提交。
-- 2) reviewed_by / reviewed_at：审核人与审核时间；写操作另记 audit_log。
-- 3) content(status, view_count) 索引已在 V5 建立，本迁移只加列，避免 MySQL 8 / H2 索引语法差异。
-- 状态机：DRAFT → PENDING → PUBLISHED / REJECTED；PUBLISHED 可被管理员 HIDDEN / ARCHIVED。
-- 注意：Flyway 已执行过的迁移不可回改内容（checksum），后续调整请新增 V9。

ALTER TABLE content ADD COLUMN review_note VARCHAR(1000);
ALTER TABLE content ADD COLUMN reviewed_by BIGINT;
ALTER TABLE content ADD COLUMN reviewed_at VARCHAR(64);
