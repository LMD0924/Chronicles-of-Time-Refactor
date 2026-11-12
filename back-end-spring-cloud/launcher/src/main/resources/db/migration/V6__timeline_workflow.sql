-- V6 时间线工作流 / 学习场次结构准备
-- 1) milestone.origin：区分「记录自动生成的里程碑(AUTO)」与「手动创建/手动关联(MANUAL)」，
--    这样 D1 双通道联动才能防重复，且手动关联不会覆盖自动来源。
-- 2) answer_records.session_id：批次 2 练习场次回看逐题使用（用户已确认加字段）。
-- 3) 回填历史数据：把 V2 种子里已标 is_milestone=1 但还没有对应里程碑的记录补齐。
-- 兼容性：仅使用 MySQL 8 与 H2 2.x 都支持的 ALTER / INSERT...SELECT / CREATE INDEX 语法。
-- 注意：Flyway 已执行过的迁移不可回改内容（checksum），后续调整请新增 V7。

ALTER TABLE milestone ADD COLUMN origin VARCHAR(16) DEFAULT 'MANUAL';
UPDATE milestone SET origin = 'MANUAL' WHERE origin IS NULL;

ALTER TABLE answer_records ADD COLUMN session_id BIGINT;

INSERT INTO milestone (user_id, title, occurred_at, description, icon, color, linked_record_id, origin, created_at)
SELECT r.user_id, r.title, r.record_date, r.content, 'star', 'mint', r.id, 'AUTO', r.created_at
FROM life_record r
LEFT JOIN milestone m ON m.linked_record_id = r.id
WHERE r.is_milestone = 1 AND m.id IS NULL;

CREATE INDEX idx_milestone_origin ON milestone(user_id, origin);
CREATE INDEX idx_answer_session ON answer_records(session_id);
