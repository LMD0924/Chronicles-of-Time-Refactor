-- V5 性能索引：把 sql/indexes.sql（原本需手工在 MySQL 上执行的参考脚本）纳入 Flyway 自动迁移。
-- 兼容性：只用标准 CREATE INDEX，去掉 MySQL 8 的 DESC 降序索引语法，
--         保证 MySQL 8.0 与 H2 2.x（dev / test profile）都能执行。
-- 校验：26 条索引的表名与列名均已逐条比对 V1__schema.sql；V1 未建任何同名索引。
-- 注意：Flyway 已执行过的迁移不可回改内容（checksum），后续调整请新增 V6。

CREATE INDEX idx_account_username ON account(username);
CREATE INDEX idx_audit_created ON audit_log(created_at);
CREATE INDEX idx_profile_user ON user_profile(user_id);
CREATE INDEX idx_resume_user ON resume_section(user_id, section_type);
CREATE INDEX idx_record_user_date ON life_record(user_id, record_date);
CREATE INDEX idx_milestone_user ON milestone(user_id, occurred_at);
CREATE INDEX idx_link_user ON growth_link(user_id);
CREATE INDEX idx_work_user ON work_record(user_id, start_date);
CREATE INDEX idx_advance_user ON advance_record(user_id, occurred_at);
CREATE INDEX idx_question_user ON question_bank(user_id, subject);
CREATE INDEX idx_answer_user ON answer_records(user_id, answered_at);
CREATE INDEX idx_mistake_user ON mistake_record(user_id, mastered);
CREATE INDEX idx_score_user ON score_record(user_id, exam_date);
CREATE INDEX idx_session_user ON practice_session(user_id, started_at);
CREATE INDEX idx_selection_user ON course_selection(user_id, status);
CREATE INDEX idx_volunteer_plan_user ON volunteer_plan(user_id, status);
CREATE INDEX idx_volunteer_detail_plan ON volunteer_detail(plan_id, priority);
CREATE INDEX idx_simulation_plan ON admission_simulation(plan_id);
CREATE INDEX idx_term_user ON term_course(user_id, term);
CREATE INDEX idx_content_user ON content(user_id, created_at);
CREATE INDEX idx_content_status ON content(status, view_count);
CREATE INDEX idx_comment_content ON content_comment(content_id);
CREATE INDEX idx_like_unique ON like_record(content_id, user_id);
CREATE INDEX idx_fav_unique ON favorite_record(content_id, user_id);
CREATE INDEX idx_paper_user ON paper(user_id, status);
CREATE INDEX idx_course_tree_parent ON course_tree_node(user_id, parent_id);
