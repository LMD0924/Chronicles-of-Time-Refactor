-- V9 演示数据扩充与既有种子对齐（批次 6 验收）
-- 1) 批次 4 审核状态机需要待审 / 驳回 / 已发布三种样例，驳回稿带可见理由。
-- 2) 批次 3 分享链接给一条未撤销、未过期的样例，便于直接打开 /share/:token 验收。
-- 3) V3 种子的 thesis_board_item / course_tree_node 用的是英文枚举，与批次 4 中文看板 / 树形页不一致，
--    这里只做精确值替换（不覆盖用户自定义的其它取值）。
-- 4) V6 加的 answer_records.session_id 在历史种子里为空，回填到该账号的第一场练习，方便验收逐题回看。
-- 兼容性：仅用 MySQL 8 / H2 2.x 都支持的 INSERT / UPDATE 语法；时间统一 VARCHAR。

INSERT INTO content (user_id, author_name, title, body, summary, category, tags, cover_url, status,
                     review_note, reviewed_by, reviewed_at, view_count, like_count, favorite_count, created_at, updated_at) VALUES
(1, '拾光用户', '等待审核的一篇复盘', '# 复盘\n\n把这一周的时间线、练习与规划放在一起看，发现record和刷题其实互相印证。',
 '把时间线、练习与规划放在一起复盘', '复盘', '时间线,复盘', NULL, 'PENDING',
 '', NULL, NULL, 0, 0, 0, '2026-11-13 09:00:00', '2026-11-13 09:00:00'),
(1, '拾光用户', '被驳回后准备改的草稿', '# 草稿\n\n标题还没想好，先把这周的事记下来。',
 '标题与背景还需要补充', '随笔', '草稿', NULL, 'REJECTED',
 '标题不够具体，请补充发生时间与背景后再提交。', 2, '2026-11-13 10:30:00',
 0, 0, 0, '2026-11-13 09:30:00', '2026-11-13 10:30:00'),
(2, '站点管理员', '站点公告：审核流程已上线', '# 公告\n\n现在投稿会先进入待审核队列，管理员通过后才会在广场出现。',
 '投稿审核流程说明', '公告', '公告,社区', NULL, 'PUBLISHED',
 '', NULL, NULL, 42, 3, 1, '2026-11-13 11:00:00', '2026-11-13 11:00:00');

INSERT INTO content_comment (content_id, author_name, body, created_at, user_id) VALUES
(1, '拾光用户', '补一句：标签聚合比按模块切分好用得多。', '2026-11-13 11:20:00', 1),
(1, '站点管理员', '已收录到文章图谱，欢迎继续补充。', '2026-11-13 11:35:00', 2);

INSERT INTO share_link (user_id, token, title, expires_at, revoked, view_count, last_viewed_at, created_at) VALUES
(1, 'demo-share-token-2026', '给家人的时间线', '2027-12-31 23:59:59', 0, 3, '2026-11-13 12:00:00', '2026-11-13 08:00:00');

INSERT INTO thesis_board_item (title, stage, deadline, status, note, user_id, created_at) VALUES
('定稿格式核对', '定稿', '2026-11-10', '待办', '已超期，需要尽快处理。', 1, '2026-11-13 09:00:00'),
('答辩幻灯片', '答辩', '2026-12-20', '进行中', '按评审意见准备三页核心结论。', 1, '2026-11-13 09:10:00');

INSERT INTO course_tree_node (parent_id, name, node_type, credit, description, sort_no, user_id, created_at) VALUES
(1, '极限与连续', '知识点', 0, '高等数学下的第一个知识点。', 1, 1, '2026-11-13 09:00:00'),
(NULL, '通识模块', '分类', 0, '跨专业通识课程。', 3, 1, '2026-11-13 09:05:00');

INSERT INTO elective_post (course_name, content, rating, created_at, user_id) VALUES
('线性代数', '概念抽象但讲义清楚，建议配合习题课。', 4.2, '2026-11-13 10:00:00', 1);

UPDATE thesis_board_item SET stage = '开题' WHERE stage = '调研';
UPDATE thesis_board_item SET stage = '初稿' WHERE stage = '开发';
UPDATE thesis_board_item SET status = '已完成' WHERE status = 'DONE';
UPDATE thesis_board_item SET status = '进行中' WHERE status = 'IN_PROGRESS';
UPDATE thesis_board_item SET status = '待办' WHERE status = 'TODO';

UPDATE course_tree_node SET node_type = '分类' WHERE node_type = 'CATEGORY';
UPDATE course_tree_node SET node_type = '课程' WHERE node_type = 'COURSE';
UPDATE course_tree_node SET node_type = '知识点' WHERE node_type IN ('KNOWLEDGE', 'POINT');

UPDATE answer_records SET session_id = (SELECT MIN(id) FROM practice_session WHERE user_id = 1)
WHERE session_id IS NULL AND user_id = 1;
