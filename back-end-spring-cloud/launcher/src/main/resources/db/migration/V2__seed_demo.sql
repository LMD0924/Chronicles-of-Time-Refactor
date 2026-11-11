-- 拾光记 · V2 演示账号与字典种子（生产部署前请删除演示账号）
INSERT INTO account (username, password_hash, nickname, role, status, token_version, bio, region, created_at, login_fail_count) VALUES
('user', 'pbkdf2$120000$Y290LWRlbW8tc2FsdC0wMQ$C5DEGX2PVc52cPAKsSZAaKRKRshIJdiIWfr09D-5Orw', '拾光用户', 'USER', 'ACTIVE', 1, '把每一天都记下来', '湖北', '2026-11-11 09:00:00', 0),
('admin', 'pbkdf2$120000$Y290LWRlbW8tc2FsdC0wMQ$eJeg7SmNJ5LLukylFflpG9BDYa2MqiCBEgY7vIaQBpQ', '站点管理员', 'ADMIN', 'ACTIVE', 1, '负责内容与账号治理', '湖北', '2026-11-11 09:00:00', 0);

INSERT INTO subject_option (name, category, is_first, is_required, description) VALUES
('语文','基础学科',0,1,'核心必修'),('数学','基础学科',0,1,'核心必修'),('英语','基础学科',0,1,'核心必修'),
('物理','理科',1,0,'首选科目'),('历史','文科',1,0,'首选科目'),('化学','理科',0,0,'再选科目'),
('生物','理科',0,0,'再选科目'),('政治','文科',0,0,'再选科目'),('地理','文科',0,0,'再选科目');

INSERT INTO grading_scale (subject_id, level, min_score, max_score, ratio, active) VALUES
(4,'A',85,100,0.15,1),(4,'B',70,84,0.35,1),(4,'C',60,69,0.30,1),(4,'D',0,59,0.20,1);

INSERT INTO major_requirement (major_code, major_name, category, level, required_subjects, description) VALUES
('080901','计算机科学与技术','工学','热门','物理,化学','演示数据'),
('100201','临床医学','医学','热门','物理,化学,生物','演示数据'),
('030101','法学','法学','常规','历史,政治','演示数据');

INSERT INTO life_record (user_id, record_date, title, content, category, mood, study_hours, sleep_hours, exercise_hours, happiness_index, is_milestone, tags, created_at) VALUES
(1,'2026-11-10','把知识点串成一张图','今天把错题和知识图谱对上了，发现薄弱点集中在函数部分。','学习','充实',4.5,7.5,1.0,8,1,'学习,复盘','2026-11-11 09:00:00'),
(1,'2026-11-08','完成一次模拟练习','练习 20 题，正确率 85%。','学习','平静',3.0,8.0,0.5,7,0,'练习','2026-11-11 09:00:00');

INSERT INTO milestone (user_id, title, occurred_at, description, icon, color, created_at) VALUES
(1,'第一次完整复盘','2026-11-10','把学习、规划、记录三条线打通了','star','amber','2026-11-11 09:00:00');

INSERT INTO score_record (user_id, subject, exam_name, exam_date, score, full_score, class_avg, created_at) VALUES
(1,'数学','期中测评','2026-11-01',88,100,76,'2026-11-11 09:00:00'),(1,'语文','期中测评','2026-11-01',82,100,79,'2026-11-11 09:00:00'),
(1,'英语','期中测评','2026-11-01',91,100,80,'2026-11-11 09:00:00'),(1,'物理','期中测评','2026-11-01',74,100,71,'2026-11-11 09:00:00');

INSERT INTO question_bank (user_id, subject, knowledge_point, title, content, options_json, answer, explanation, difficulty, source, created_at) VALUES
(1,'数学','函数','函数的单调性判断','下列说法正确的是？','["A. 增","B. 减","C. 常","D. 不确定"]','A','由导数符号判断','中等','自建','2026-11-11 09:00:00');

INSERT INTO content (user_id, author_name, title, body, summary, category, tags, status, view_count, like_count, favorite_count, created_at, updated_at) VALUES
(1,'拾光用户','把记录连成时间线之后','以前功能之间互相孤立，现在一条时间轴把学习、规划与档案串起来了。','一次关于信息架构的复盘','随笔','复盘,时间线','PUBLISHED',128,12,4,'2026-11-11 09:00:00','2026-11-11 09:00:00');

INSERT INTO term_course (user_id, term, course_name, credit, score, gpa_point, category, created_at) VALUES
(1,'2026-1','高等数学',4,88,3.7,'必修','2026-11-11 09:00:00'),(1,'2026-1','程序设计',3,92,4.0,'必修','2026-11-11 09:00:00');

INSERT INTO graduation_gap (user_id, requirement_name, required_credit, earned_credit, status, note) VALUES
(1,'通识学分',12,8,'进行中','还差 4 学分');
