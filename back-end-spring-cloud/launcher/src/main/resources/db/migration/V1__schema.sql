-- 拾光记 · V1 全量建表（表名沿用老项目，保证数据可迁移）
-- 单库 chronicles_of_time；所有业务表按 user_id 隔离。

-- ── 限界上下文：身份与访问 (identity) ──
CREATE TABLE IF NOT EXISTS account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255),
    password_hash VARCHAR(255),
    nickname VARCHAR(255),
    role VARCHAR(255),
    status VARCHAR(255),
    token_version INT,
    avatar_url VARCHAR(255),
    bio TEXT,
    region VARCHAR(255),
    created_at VARCHAR(255),
    last_login_at VARCHAR(255),
    login_fail_count INT,
    locked_until BIGINT
);
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT,
    operator_name VARCHAR(255),
    action VARCHAR(255),
    target_type VARCHAR(255),
    target_id VARCHAR(255),
    before_value TEXT,
    after_value TEXT,
    created_at VARCHAR(255)
);
CREATE TABLE IF NOT EXISTS user_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    real_name VARCHAR(255),
    phone VARCHAR(255),
    email VARCHAR(255),
    school VARCHAR(255),
    bio TEXT,
    birthday VARCHAR(255),
    updated_at VARCHAR(255),
    user_id BIGINT,
    created_at VARCHAR(64)
);
CREATE TABLE IF NOT EXISTS resume_section (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_type VARCHAR(255),
    title VARCHAR(255),
    subtitle VARCHAR(255),
    content TEXT,
    start_date VARCHAR(255),
    end_date VARCHAR(255),
    sort_no INT,
    created_at VARCHAR(255),
    user_id BIGINT
);

-- ── 限界上下文：拾光时间线 (timeline) ──
CREATE TABLE IF NOT EXISTS life_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_date VARCHAR(255),
    title VARCHAR(255),
    content TEXT,
    category VARCHAR(255),
    period VARCHAR(255),
    mood VARCHAR(255),
    study_hours DOUBLE,
    sleep_hours DOUBLE,
    exercise_hours DOUBLE,
    happiness_index INT,
    is_milestone TINYINT(1),
    tags TEXT,
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS milestone (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    occurred_at VARCHAR(255),
    description TEXT,
    icon VARCHAR(255),
    color VARCHAR(255),
    linked_record_id BIGINT,
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS growth_link (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_node VARCHAR(255),
    to_node VARCHAR(255),
    relation VARCHAR(255),
    score DOUBLE,
    note TEXT,
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS work_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company VARCHAR(255),
    position VARCHAR(255),
    start_date VARCHAR(255),
    end_date VARCHAR(255),
    content TEXT,
    category VARCHAR(255),
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS advance_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    occurred_at VARCHAR(255),
    category VARCHAR(255),
    description TEXT,
    created_at VARCHAR(255),
    user_id BIGINT
);

-- ── 限界上下文：学习 (learning) ──
CREATE TABLE IF NOT EXISTS question_bank (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject VARCHAR(255),
    knowledge_point VARCHAR(255),
    title VARCHAR(255),
    content TEXT,
    options_json TEXT,
    answer VARCHAR(255),
    explanation TEXT,
    difficulty VARCHAR(255),
    source VARCHAR(255),
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS answer_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT,
    user_answer VARCHAR(255),
    correct TINYINT(1),
    score DOUBLE,
    duration_ms BIGINT,
    answered_at VARCHAR(255),
    user_id BIGINT,
    created_at VARCHAR(64)
);
CREATE TABLE IF NOT EXISTS mistake_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT,
    knowledge_point VARCHAR(255),
    reason TEXT,
    mastered TINYINT(1),
    review_count INT,
    last_review_at VARCHAR(255),
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS score_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject VARCHAR(255),
    exam_name VARCHAR(255),
    exam_date VARCHAR(255),
    score DOUBLE,
    full_score DOUBLE,
    class_avg DOUBLE,
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS practice_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    question_count INT,
    correct_count INT,
    started_at VARCHAR(255),
    finished_at VARCHAR(255),
    user_id BIGINT,
    created_at VARCHAR(64)
);

-- ── 限界上下文：规划 (planning) ──
CREATE TABLE IF NOT EXISTS subject_option (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    category VARCHAR(255),
    is_first TINYINT(1),
    is_required TINYINT(1),
    description TEXT,
    created_at VARCHAR(64)
);
CREATE TABLE IF NOT EXISTS subject_combination (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_subject VARCHAR(255),
    second_subjects TEXT,
    label VARCHAR(255),
    student_count INT,
    coverage DOUBLE,
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS course_selection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    combination_id BIGINT,
    status VARCHAR(255),
    confirmed_at VARCHAR(255),
    note TEXT,
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS selection_intention (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_major VARCHAR(255),
    target_school VARCHAR(255),
    reason TEXT,
    status VARCHAR(255),
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS course_guidance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    content TEXT,
    status VARCHAR(255),
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS selection_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    selection_id BIGINT,
    action VARCHAR(255),
    operator VARCHAR(255),
    detail TEXT,
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS grading_scale (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT,
    level VARCHAR(255),
    min_score DOUBLE,
    max_score DOUBLE,
    ratio DOUBLE,
    active TINYINT(1),
    created_at VARCHAR(64)
);
CREATE TABLE IF NOT EXISTS major_requirement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    major_code VARCHAR(255),
    major_name VARCHAR(255),
    category VARCHAR(255),
    level VARCHAR(255),
    required_subjects TEXT,
    description TEXT,
    created_at VARCHAR(64)
);
CREATE TABLE IF NOT EXISTS volunteer_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    plan_year VARCHAR(255),
    province VARCHAR(255),
    status VARCHAR(255),
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS volunteer_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT,
    university_name VARCHAR(255),
    major_name VARCHAR(255),
    priority INT,
    chance_level VARCHAR(255),
    score_line DOUBLE,
    note TEXT,
    user_id BIGINT,
    created_at VARCHAR(64)
);
CREATE TABLE IF NOT EXISTS admission_simulation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT,
    detail_id BIGINT,
    result VARCHAR(255),
    probability DOUBLE,
    analysis TEXT,
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS term_course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    term VARCHAR(255),
    course_name VARCHAR(255),
    credit DOUBLE,
    score DOUBLE,
    gpa_point DOUBLE,
    category VARCHAR(255),
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS graduation_gap (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requirement_name VARCHAR(255),
    required_credit DOUBLE,
    earned_credit DOUBLE,
    status VARCHAR(255),
    note TEXT,
    user_id BIGINT,
    created_at VARCHAR(64)
);
CREATE TABLE IF NOT EXISTS review_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject VARCHAR(255),
    title VARCHAR(255),
    content TEXT,
    target_date VARCHAR(255),
    progress INT,
    status VARCHAR(255),
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS target_path (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_name VARCHAR(255),
    stage VARCHAR(255),
    milestone VARCHAR(255),
    deadline VARCHAR(255),
    status VARCHAR(255),
    note TEXT,
    user_id BIGINT,
    created_at VARCHAR(64)
);
CREATE TABLE IF NOT EXISTS quality_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    category VARCHAR(255),
    score DOUBLE,
    occurred_at VARCHAR(255),
    description TEXT,
    user_id BIGINT,
    created_at VARCHAR(64)
);
CREATE TABLE IF NOT EXISTS checklist_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    category VARCHAR(255),
    done TINYINT(1),
    deadline VARCHAR(255),
    note TEXT,
    user_id BIGINT,
    created_at VARCHAR(64)
);

-- ── 限界上下文：档案 (archive) ──
CREATE TABLE IF NOT EXISTS certificate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    issuer VARCHAR(255),
    issued_at VARCHAR(255),
    file_url VARCHAR(255),
    category VARCHAR(255),
    user_id BIGINT,
    created_at VARCHAR(64)
);
CREATE TABLE IF NOT EXISTS paper (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    abstract_text TEXT,
    keywords TEXT,
    status VARCHAR(255),
    advisor VARCHAR(255),
    file_url VARCHAR(255),
    created_at VARCHAR(255),
    updated_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS paper_suggestion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    paper_id BIGINT,
    author VARCHAR(255),
    content TEXT,
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS course_tree_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT,
    name VARCHAR(255),
    node_type VARCHAR(255),
    credit DOUBLE,
    description TEXT,
    sort_no INT,
    user_id BIGINT,
    created_at VARCHAR(64)
);
CREATE TABLE IF NOT EXISTS thesis_board_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    stage VARCHAR(255),
    deadline VARCHAR(255),
    status VARCHAR(255),
    note TEXT,
    user_id BIGINT,
    created_at VARCHAR(64)
);
CREATE TABLE IF NOT EXISTS elective_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_name VARCHAR(255),
    content TEXT,
    rating DOUBLE,
    created_at VARCHAR(255),
    user_id BIGINT
);

-- ── 限界上下文：社区 (community) ──
CREATE TABLE IF NOT EXISTS content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    author_name VARCHAR(255),
    title VARCHAR(255),
    body TEXT,
    summary TEXT,
    category VARCHAR(255),
    tags TEXT,
    cover_url VARCHAR(255),
    status VARCHAR(255),
    view_count INT,
    like_count INT,
    favorite_count INT,
    created_at VARCHAR(255),
    updated_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS content_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id BIGINT,
    author_name VARCHAR(255),
    body TEXT,
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS like_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id BIGINT,
    created_at VARCHAR(255),
    user_id BIGINT
);
CREATE TABLE IF NOT EXISTS favorite_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id BIGINT,
    created_at VARCHAR(255),
    user_id BIGINT
);
