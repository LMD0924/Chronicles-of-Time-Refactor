package com.chroniclesoftime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 全链路集成测试：H2 内存库 + 与生产完全相同的 Flyway 迁移（V1~V5）+ 完整 Spring 上下文。
 * 不依赖本机 MySQL / Redis，可直接被 mvn test 与 GitHub Actions 执行。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"spring.profiles.active=test", "COT_PROFILE=test"})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CotApplicationIntegrationTest {

    private static final String USER = "user";
    private static final String USER_PW = "Cot@2026user";
    private static final String ADMIN = "admin";
    private static final String ADMIN_PW = "Cot@2026admin";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;

    private JsonNode loginData(String username, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode data = json.readTree(r.getResponse().getContentAsString()).path("data");
        assertFalse(data.path("token").asText().isBlank(), "登录必须返回 token");
        return data;
    }

    private String login(String username, String password) throws Exception {
        return loginData(username, password).path("token").asText();
    }

    private JsonNode getJson(String path, String token, int expectedStatus) throws Exception {
        MvcResult r = mvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    @Test @Order(1)
    void flywayMigrationsAndSeedDataAreLoaded() {
        Integer accounts = jdbc.queryForObject("select count(*) from account", Integer.class);
        assertTrue(accounts != null && accounts >= 2, "演示账号应由 Flyway 写入");

        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);
        assertTrue(applied != null && applied >= 5, "V1~V5 迁移应全部成功，实际 " + applied);

        Integer milestones = jdbc.queryForObject("select count(*) from milestone", Integer.class);
        assertTrue(milestones != null && milestones > 0, "种子数据应可见");

        // V5 索引确实建出来了（H2 的 DATABASE_TO_LOWER 会让索引名大小写不同，故统一转大写比较）
        Integer indexes = jdbc.queryForObject(
                "select count(*) from information_schema.indexes "
                        + "where upper(index_name) like 'IDX_%'", Integer.class);
        assertTrue(indexes != null && indexes > 0, "V5 性能索引应已创建");
    }

    @Test @Order(2)
    void demoAccountsCanLogin() throws Exception {
        assertEquals("USER", loginData(USER, USER_PW).path("role").asText());
        assertEquals("ADMIN", loginData(ADMIN, ADMIN_PW).path("role").asText());
    }

    @Test @Order(3)
    void anonymousAndCrossRoleAccessAreRejected() throws Exception {
        mvc.perform(get("/api/v1/timeline/feed"))
                .andExpect(status().isUnauthorized());

        String userToken = login(USER, USER_PW);
        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // 公开只读接口无需登录
        mvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk());
    }

    @Test @Order(4)
    void adminApisWorkWithAdminToken() throws Exception {
        String admin = login(ADMIN, ADMIN_PW);
        JsonNode users = getJson("/api/v1/admin/users", admin, 200);
        assertEquals(200, users.path("code").asInt());
        assertTrue(users.path("data").isArray());

        JsonNode overview = getJson("/api/v1/admin/overview", admin, 200);
        assertEquals(200, overview.path("code").asInt());
        assertTrue(overview.path("data").isObject());

        JsonNode dictionaries = getJson("/api/v1/admin/dictionaries", admin, 200);
        assertEquals(200, dictionaries.path("code").asInt());
    }

    @Test @Order(5)
    void crudWriteAndDeleteAreAccountScoped() throws Exception {
        JsonNode userData = loginData(USER, USER_PW);
        String user = userData.path("token").asText();
        long uid = userData.path("userId").asLong();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "集成测试里程碑");
        body.put("description", "由集成测试创建，随后删除");
        MvcResult r = mvc.perform(post("/api/v1/timeline/r/milestones")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        long id = json.readTree(r.getResponse().getContentAsString()).path("data").path("id").asLong();
        assertTrue(id > 0, "新增应返回主键");

        Long owner = jdbc.queryForObject("select user_id from milestone where id = ?", Long.class, id);
        assertEquals(uid, owner == null ? -1 : owner, "新增记录必须归属当前登录用户");

        mvc.perform(delete("/api/v1/timeline/r/milestones/" + id)
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        Integer left = jdbc.queryForObject("select count(*) from milestone where id = ?", Integer.class, id);
        assertEquals(0, left, "删除应真正落库");
    }

    @Test @Order(6)
    void listIsAccountScopedWhileAdminCanAuditAll() throws Exception {
        String user = login(USER, USER_PW);
        String admin = login(ADMIN, ADMIN_PW);

        JsonNode mine = getJson("/api/v1/timeline/r/milestones", user, 200).path("data");
        assertTrue(mine.isArray() && !mine.isEmpty(), "普通用户应能看到自己的里程碑");

        JsonNode all = getJson("/api/v1/timeline/r/milestones?all=true", admin, 200).path("data");
        assertTrue(all.isArray());
        assertTrue(all.size() >= mine.size(), "管理员 all=true 核查范围应不小于普通用户");
    }

    @Test @Order(7)
    void approvalStateMachineWritesHistoryAndAudit() throws Exception {
        String admin = login(ADMIN, ADMIN_PW);
        JsonNode pending = getJson("/api/v1/planning/selection/approvals/pending", admin, 200).path("data");
        assertTrue(pending.isArray() && !pending.isEmpty(), "演示数据应包含待审批申请");
        long id = pending.get(0).path("id").asLong();

        mvc.perform(put("/api/v1/planning/selection/approvals/" + id)
                        .param("decision", "APPROVE")
                        .param("comment", "集成测试审批")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String status = jdbc.queryForObject(
                "select status from course_selection where id = ?", String.class, id);
        assertEquals("APPROVED", status, "审批应真正改变状态");

        Integer audits = jdbc.queryForObject(
                "select count(*) from audit_log where action = 'APPROVE_SELECTION'", Integer.class);
        assertTrue(audits != null && audits >= 1, "审批应写入审计日志");

        Integer histories = jdbc.queryForObject(
                "select count(*) from selection_history where selection_id = ? and action = 'APPROVE'",
                Integer.class, id);
        assertTrue(histories != null && histories >= 1, "审批应写入变更历史");

        // 状态机约束：非 PENDING 不允许重复处理
        mvc.perform(put("/api/v1/planning/selection/approvals/" + id)
                        .param("decision", "APPROVE")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test @Order(8)
    void logoutInvalidatesIssuedToken() throws Exception {
        String token = login(USER, USER_PW);
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // token_version 递增后，旧令牌应立即失效
        mvc.perform(get("/api/v1/timeline/feed").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    // ---------- 批次 1：拾光时间线核心闭环 ----------

    @Test @Order(9)
    void v6MigrationAddsWorkflowColumnsAndBackfills() {
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);
        assertTrue(applied != null && applied >= 6, "V1~V6 迁移应全部成功，实际 " + applied);

        Integer origin = jdbc.queryForObject(
                "select count(*) from information_schema.columns where upper(table_name) = 'MILESTONE' "
                        + "and upper(column_name) = 'ORIGIN'", Integer.class);
        assertEquals(1, origin, "V6 应给 milestone 增加 origin 列");

        Integer sessionCol = jdbc.queryForObject(
                "select count(*) from information_schema.columns where upper(table_name) = 'ANSWER_RECORDS' "
                        + "and upper(column_name) = 'SESSION_ID'", Integer.class);
        assertEquals(1, sessionCol, "V6 应给 answer_records 增加 session_id 列");

        Integer backfilled = jdbc.queryForObject(
                "select count(*) from milestone where origin = 'AUTO'", Integer.class);
        assertTrue(backfilled != null && backfilled >= 1, "V6 应回填历史 is_milestone 记录");
    }

    @Test @Order(10)
    void recordFlagCreatesAndRecyclesAutoMilestone() throws Exception {
        String user = login(USER, USER_PW);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("recordDate", "2026-11-12");
        body.put("title", "批次1自动里程碑测试");
        body.put("content", "联动验证");
        body.put("isMilestone", true);

        MvcResult r = mvc.perform(post("/api/v1/timeline/r/records")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        long rid = json.readTree(r.getResponse().getContentAsString()).path("data").path("id").asLong();
        assertTrue(rid > 0);

        String countSql = "select count(*) from milestone where linked_record_id = ? and origin = 'AUTO'";
        assertEquals(1, jdbc.queryForObject(countSql, Integer.class, rid), "勾选后应自动生成一条 AUTO 里程碑");

        // 重复保存不得产生第二条
        Map<String, Object> again = new LinkedHashMap<>(body);
        again.put("id", rid);
        mvc.perform(put("/api/v1/timeline/r/records/" + rid)
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(again)))
                .andExpect(status().isOk());
        assertEquals(1, jdbc.queryForObject(countSql, Integer.class, rid), "联动必须幂等");

        // 取消勾选后回收
        again.put("isMilestone", false);
        mvc.perform(put("/api/v1/timeline/r/records/" + rid)
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(again)))
                .andExpect(status().isOk());
        assertEquals(0, jdbc.queryForObject(countSql, Integer.class, rid), "取消勾选应回收 AUTO 里程碑");

        mvc.perform(delete("/api/v1/timeline/r/records/" + rid)
                .header("Authorization", "Bearer " + user))
                .andExpect(status().isOk());
    }

    @Test @Order(11)
    void manualMilestoneCanLinkAndUnlinkRecord() throws Exception {
        String user = login(USER, USER_PW);
        long recordId = createRecord(user, "手动关联用记录", false, "LINK_PROBE_CAT");
        long milestoneId = createMilestone(user, "手动创建的里程碑");
        try {
            mvc.perform(post("/api/v1/timeline/milestones/" + milestoneId + "/link")
                            .header("Authorization", "Bearer " + user)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("recordId", recordId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
            Long linked = jdbc.queryForObject("select linked_record_id from milestone where id = ?", Long.class, milestoneId);
            assertEquals(recordId, linked == null ? -1 : linked, "手动关联应写入 linked_record_id");
            assertEquals("MANUAL", jdbc.queryForObject("select origin from milestone where id = ?", String.class, milestoneId));

            mvc.perform(delete("/api/v1/timeline/milestones/" + milestoneId + "/link")
                            .header("Authorization", "Bearer " + user))
                    .andExpect(status().isOk());
            assertNull(jdbc.queryForObject("select linked_record_id from milestone where id = ?", Long.class, milestoneId),
                    "解除关联必须真正把 linked_record_id 清空");
        } finally {
            mvc.perform(delete("/api/v1/timeline/r/milestones/" + milestoneId)
                    .header("Authorization", "Bearer " + user)).andExpect(status().isOk());
            mvc.perform(delete("/api/v1/timeline/r/records/" + recordId)
                    .header("Authorization", "Bearer " + user)).andExpect(status().isOk());
        }
    }

    @Test @Order(12)
    void feedFiltersAndPaginationWork() throws Exception {
        String user = login(USER, USER_PW);
        JsonNode page = getJson("/api/v1/timeline/feed/page?page=1&size=3", user, 200).path("data");
        assertTrue(page.path("items").isArray());
        assertTrue(page.path("items").size() <= 3, "分页应限制每页条数");
        assertTrue(page.path("total").asInt() >= page.path("items").size());
        assertTrue(page.path("pages").asInt() >= 1);

        JsonNode empty = getJson("/api/v1/timeline/feed/page?q=zzz_no_such_keyword_zzz", user, 200).path("data");
        assertEquals(0, empty.path("total").asInt(), "关键词筛不出结果时总数应为 0");

        JsonNode future = getJson("/api/v1/timeline/feed?dateFrom=2099-01-01", user, 200).path("data");
        assertTrue(future.isArray() && future.isEmpty(), "起始日期筛选应生效");

        JsonNode limited = getJson("/api/v1/timeline/feed?limit=2", user, 200).path("data");
        assertTrue(limited.isArray() && limited.size() <= 2, "limit 应生效");
    }

    @Test @Order(13)
    void workflowSyncsWorkRecordIntoResume() throws Exception {
        String user = login(USER, USER_PW);
        Map<String, Object> work = new LinkedHashMap<>();
        work.put("company", "批次1同步测试单位");
        work.put("position", "测试岗");
        work.put("startDate", "2026-11-01");
        work.put("category", "实习");
        MvcResult r = mvc.perform(post("/api/v1/timeline/r/work-records")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(work)))
                .andExpect(status().isOk()).andReturn();
        long wid = json.readTree(r.getResponse().getContentAsString()).path("data").path("id").asLong();

        mvc.perform(post("/api/v1/timeline/workflow/sync-resume")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("kind", "work", "ids", java.util.List.of(wid)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.created").value(1));

        JsonNode status = getJson("/api/v1/timeline/workflow/resume-status", user, 200).path("data");
        assertTrue(status.path("work").asInt() >= 1, "简历分组应可见回写结果");

        // 再次同步应跳过同名分组，不重复写入
        mvc.perform(post("/api/v1/timeline/workflow/sync-resume")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("kind", "work", "ids", java.util.List.of(wid)))))
                .andExpect(jsonPath("$.data.skipped").value(1));

        mvc.perform(delete("/api/v1/timeline/r/work-records/" + wid)
                .header("Authorization", "Bearer " + user)).andExpect(status().isOk());
    }

    @Test @Order(14)
    void featureEndpointsAreAccountScoped() throws Exception {
        String admin = login(ADMIN, ADMIN_PW);
        String onlyAdminCategory = "SCOPE_PROBE_CAT";
        long adminRecord = createRecord(admin, "ADMIN_SCOPED_RECORD", false, onlyAdminCategory);
        try {
            String user = login(USER, USER_PW);
            JsonNode facets = getJson("/api/v1/timeline/records/facets", user, 200).path("data");
            assertFalse(facets.path("categories").toString().contains(onlyAdminCategory),
                    "普通用户的筛选字典不得包含其他账号的数据");

            JsonNode feed = getJson("/api/v1/timeline/feed?q=ADMIN_SCOPED_RECORD", user, 200).path("data");
            assertEquals(0, feed.size(), "时间线不得泄露其他账号的记录");
        } finally {
            mvc.perform(delete("/api/v1/timeline/r/records/" + adminRecord)
                    .header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
        }
    }

    private long createRecord(String token, String title, boolean milestone, String category) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("recordDate", "2026-11-12");
        body.put("title", title);
        body.put("category", category);
        body.put("isMilestone", milestone);
        MvcResult r = mvc.perform(post("/api/v1/timeline/r/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private long createMilestone(String token, String title) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/timeline/r/milestones")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", title, "occurredAt", "2026-11-12"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    // ---------- 批次 2：学习完整链路 ----------
    private static long probeSessionId;
    private static long probeMistakeId;

    @Test @Order(15)
    void practiceWorkflowSyncsSessionAnswersAndMistakes() throws Exception {
        String user = login(USER, USER_PW);
        List<Map<String, Object>> imported = List.of(
                question("BATCH2_PROBE", "KP_A", "Q1", "A"),
                question("BATCH2_PROBE", "KP_B", "Q2", "B"));
        mvc.perform(post("/api/v1/learning/questions/import")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("questions", imported))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.created").value(2));

        Map<String, Object> start = new LinkedHashMap<>();
        start.put("subject", "BATCH2_PROBE"); start.put("size", 2); start.put("title", "批次2联动测试");
        MvcResult r = mvc.perform(post("/api/v1/learning/practice/start")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(start)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode data = json.readTree(r.getResponse().getContentAsString()).path("data");
        probeSessionId = data.path("sessionId").asLong();
        JsonNode qs = data.path("questions");
        assertTrue(probeSessionId > 0, "应创建练习场次");
        assertEquals(2, qs.size());
        assertFalse(qs.get(0).has("answer"), "组卷返回的题目不得包含答案");

        List<Map<String, Object>> answers = new ArrayList<>();
        for (JsonNode q : qs) {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("questionId", q.path("id").asLong());
            a.put("userAnswer", "Q1".equals(q.path("title").asText()) ? "A" : "Z");
            a.put("durationMs", 1200);
            answers.add(a);
        }
        mvc.perform(post("/api/v1/learning/practice/" + probeSessionId + "/submit")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("answers", answers))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.correctCount").value(1))
                .andExpect(jsonPath("$.data.results.length()").value(2));

        assertEquals(1, jdbc.queryForObject("select correct_count from practice_session where id = ?",
                Integer.class, probeSessionId), "场次正确数应回填");
        assertNotNull(jdbc.queryForObject("select finished_at from practice_session where id = ?",
                String.class, probeSessionId), "交卷后应有结束时间");
        assertEquals(2, jdbc.queryForObject("select count(*) from answer_records where session_id = ?",
                Integer.class, probeSessionId), "逐题记录必须带上 session_id");
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from mistake_record where question_id in (select id from question_bank where subject = 'BATCH2_PROBE')",
                Integer.class), "答错一题应只生成一条错题");
        probeMistakeId = jdbc.queryForObject(
                "select id from mistake_record where question_id in (select id from question_bank where subject = 'BATCH2_PROBE') limit 1",
                Long.class);

        // 状态机：同一场次不得重复交卷
        mvc.perform(post("/api/v1/learning/practice/" + probeSessionId + "/submit")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("answers", answers))))
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test @Order(16)
    void sessionDetailReplaysEachQuestion() throws Exception {
        String user = login(USER, USER_PW);
        JsonNode data = getJson("/api/v1/learning/practice/sessions/" + probeSessionId, user, 200).path("data");
        assertEquals(2, data.path("items").size(), "场次回看应带出两道题的记录");
        boolean hasAnswer = false;
        for (JsonNode item : data.path("items")) if (item.has("answer")) hasAnswer = true;
        assertTrue(hasAnswer, "回看应 join 出正确答案与解析");
        JsonNode sessions = getJson("/api/v1/learning/practice/sessions", user, 200).path("data");
        assertTrue(sessions.isArray() && !sessions.isEmpty());
    }

    @Test @Order(17)
    void mistakeReviewAndMasteryToggle() throws Exception {
        String user = login(USER, USER_PW);
        int before = jdbc.queryForObject("select review_count from mistake_record where id = ?",
                Integer.class, probeMistakeId);
        mvc.perform(post("/api/v1/learning/mistakes/" + probeMistakeId + "/review")
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        assertEquals(before + 1, jdbc.queryForObject("select review_count from mistake_record where id = ?",
                Integer.class, probeMistakeId), "复习次数应累加");

        mvc.perform(put("/api/v1/learning/mistakes/" + probeMistakeId + "/mastered")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mastered\":true}"))
                .andExpect(jsonPath("$.code").value(200));
        assertEquals(1, jdbc.queryForObject("select count(*) from mistake_record where id = ? and mastered = 1",
                Integer.class, probeMistakeId), "应标记为已掌握");

        mvc.perform(put("/api/v1/learning/mistakes/" + probeMistakeId + "/mastered")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mastered\":false}"))
                .andExpect(jsonPath("$.code").value(200));
        assertEquals(0, jdbc.queryForObject("select count(*) from mistake_record where id = ? and mastered = 1",
                Integer.class, probeMistakeId), "应能移回待复习");
    }

    @Test @Order(18)
    void questionImportReportsPerItemErrors() throws Exception {
        String user = login(USER, USER_PW);
        Map<String, Object> bad = new LinkedHashMap<>();
        bad.put("subject", "BATCH2_IMPORT"); bad.put("title", "缺少答案的题目");
        List<Map<String, Object>> payload = List.of(question("BATCH2_IMPORT", "KP_C", "Q3", "A"), bad);
        mvc.perform(post("/api/v1/learning/questions/import")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("questions", payload))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.created").value(1))
                .andExpect(jsonPath("$.data.failed").value(1))
                .andExpect(jsonPath("$.data.errors[0].reason").exists());
    }

    @Test @Order(19)
    void learningFeatureEndpointsAreAccountScoped() throws Exception {
        String admin = login(ADMIN, ADMIN_PW);
        mvc.perform(post("/api/v1/learning/questions/import")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("questions",
                                List.of(question("ADMIN_LEARNING_ONLY", "KP_X", "ADMIN_Q", "A"))))))
                .andExpect(jsonPath("$.data.created").value(1));
        try {
            String user = login(USER, USER_PW);
            JsonNode facets = getJson("/api/v1/learning/questions/facets", user, 200).path("data");
            assertFalse(facets.path("subjects").toString().contains("ADMIN_LEARNING_ONLY"),
                    "普通用户的题库字典不得包含其他账号的数据");
            JsonNode random = getJson("/api/v1/learning/random-questions?subject=ADMIN_LEARNING_ONLY&size=5", user, 200).path("data");
            assertEquals(0, random.size(), "随机抽题不得泄露其他账号题目");
        } finally {
            jdbc.update("delete from question_bank where subject = 'ADMIN_LEARNING_ONLY'");
        }
    }

    // ---------------------------------------------------------------- 批次 3：规划工作流与免登录分享

    @Test @Order(20)
    void shareLinkCanBeCreatedAndViewedWithoutLogin() throws Exception {
        assertNotNull(jdbc.queryForObject("select count(*) from share_link", Integer.class), "V7 应建好 share_link 表");
        String user = login(USER, USER_PW);
        MvcResult r = mvc.perform(post("/api/v1/planning/share-links")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "BATCH3_SHARE", "days", 7))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode data = json.readTree(r.getResponse().getContentAsString()).path("data");
        String token = data.path("token").asText();
        long id = data.path("id").asLong();
        assertFalse(token.isBlank(), "必须返回随机 token");
        assertTrue(data.path("url").asText().endsWith(token));
        try {
            // 完全不带 Authorization：公开只读端点应放行，且只返回汇总与节点
            MvcResult view = mvc.perform(get("/api/v1/timeline/share/" + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.title").value("BATCH3_SHARE"))
                    .andReturn();
            JsonNode body = json.readTree(view.getResponse().getContentAsString()).path("data");
            assertTrue(body.path("stats").has("records"), "应返回汇总统计");
            assertFalse(body.toString().contains("userId"), "公开视图不得泄露 user_id");
            assertFalse(body.toString().contains("password"), "公开视图不得泄露账号信息");
            assertEquals(1, jdbc.queryForObject("select view_count from share_link where id = ?", Integer.class, id),
                    "每次公开访问应累加访问次数");
        } finally {
            jdbc.update("delete from share_link where id = ?", id);
        }
    }

    @Test @Order(21)
    void revokedExpiredAndDeletedShareLinksAreRejected() throws Exception {
        String user = login(USER, USER_PW);
        MvcResult r = mvc.perform(post("/api/v1/planning/share-links")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"BATCH3_LIFECYCLE\",\"days\":7}"))
                .andExpect(jsonPath("$.code").value(200)).andReturn();
        JsonNode data = json.readTree(r.getResponse().getContentAsString()).path("data");
        String token = data.path("token").asText();
        long id = data.path("id").asLong();
        try {
            mvc.perform(put("/api/v1/planning/share-links/" + id + "/revoke")
                            .header("Authorization", "Bearer " + user))
                    .andExpect(jsonPath("$.data.revoked").value(true));
            mvc.perform(get("/api/v1/timeline/share/" + token))
                    .andExpect(jsonPath("$.code").value(404));

            mvc.perform(put("/api/v1/planning/share-links/" + id + "/renew")
                            .header("Authorization", "Bearer " + user)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"days\":7}"))
                    .andExpect(jsonPath("$.data.revoked").value(false));
            mvc.perform(get("/api/v1/timeline/share/" + token))
                    .andExpect(jsonPath("$.code").value(200));

            jdbc.update("update share_link set expires_at = ? where id = ?", "2020-01-01 00:00:00", id);
            mvc.perform(get("/api/v1/timeline/share/" + token))
                    .andExpect(jsonPath("$.code").value(404));

            mvc.perform(delete("/api/v1/planning/share-links/" + id)
                            .header("Authorization", "Bearer " + user))
                    .andExpect(jsonPath("$.data.deleted").value(true));
            assertEquals(0, jdbc.queryForObject("select count(*) from share_link where id = ?", Integer.class, id));
        } finally {
            jdbc.update("delete from share_link where id = ?", id);
        }
    }

    @Test @Order(22)
    void shareLinksAndPlanningFeaturesAreAccountScoped() throws Exception {
        String admin = login(ADMIN, ADMIN_PW);
        MvcResult r = mvc.perform(post("/api/v1/planning/share-links")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"ADMIN_BATCH3_SHARE\",\"days\":7}"))
                .andExpect(jsonPath("$.code").value(200)).andReturn();
        JsonNode data = json.readTree(r.getResponse().getContentAsString()).path("data");
        String adminToken = data.path("token").asText();
        long linkId = data.path("id").asLong();
        long planId = createReviewPlan(admin, "ADMIN_BATCH3_PLAN");
        try {
            String user = login(USER, USER_PW);
            JsonNode links = getJson("/api/v1/planning/share-links", user, 200).path("data");
            assertFalse(links.toString().contains(adminToken), "分享链接列表不得泄露其他账号的链接");
            mvc.perform(put("/api/v1/planning/share-links/" + linkId + "/revoke")
                            .header("Authorization", "Bearer " + user))
                    .andExpect(jsonPath("$.code").value(403));
            JsonNode board = getJson("/api/v1/planning/workflow/review/board", user, 200).path("data");
            assertFalse(board.path("items").toString().contains("ADMIN_BATCH3_PLAN"),
                    "复习看板不得泄露其他账号的计划");
            JsonNode overview = getJson("/api/v1/planning/workflow/overview", user, 200).path("data");
            assertFalse(overview.toString().contains("ADMIN_BATCH3_PLAN"));
        } finally {
            jdbc.update("delete from share_link where id = ?", linkId);
            jdbc.update("delete from review_plan where id = ?", planId);
        }
    }

    @Test @Order(23)
    void reviewProgressAndMilestoneDecomposition() throws Exception {
        String user = login(USER, USER_PW);
        long id = createReviewPlan(user, "BATCH3_REVIEW");
        String note = "由复习计划 #" + id + " 分解生成";
        try {
            mvc.perform(put("/api/v1/planning/workflow/review/" + id + "/progress")
                            .header("Authorization", "Bearer " + user)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"progress\":100}"))
                    .andExpect(jsonPath("$.code").value(200));
            assertEquals(100, jdbc.queryForObject("select progress from review_plan where id = ?", Integer.class, id));
            assertEquals("已完成", jdbc.queryForObject("select status from review_plan where id = ?", String.class, id),
                    "进度 100% 应自动置为已完成");

            mvc.perform(post("/api/v1/planning/workflow/review/" + id + "/milestones")
                            .header("Authorization", "Bearer " + user)
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(jsonPath("$.data.created").value(4));
            assertEquals(4, jdbc.queryForObject("select count(*) from target_path where note = ?", Integer.class, note));

            mvc.perform(put("/api/v1/planning/workflow/review/" + id + "/progress")
                            .header("Authorization", "Bearer " + user)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"progress\":0}"))
                    .andExpect(jsonPath("$.code").value(200));
            assertEquals("未开始", jdbc.queryForObject("select status from review_plan where id = ?", String.class, id));
        } finally {
            jdbc.update("delete from target_path where note = ?", note);
            jdbc.update("delete from review_plan where id = ?", id);
        }
    }

    @Test @Order(24)
    void checklistToggleAndGradingConversion() throws Exception {
        String user = login(USER, USER_PW);
        MvcResult r = mvc.perform(post("/api/v1/planning/r/checklist-items")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"BATCH3_CHECK\",\"category\":\"集成测试\",\"done\":false}"))
                .andExpect(jsonPath("$.code").value(200)).andReturn();
        long id = json.readTree(r.getResponse().getContentAsString()).path("data").path("id").asLong();
        try {
            mvc.perform(put("/api/v1/planning/workflow/checklist/" + id + "/toggle")
                            .header("Authorization", "Bearer " + user))
                    .andExpect(jsonPath("$.data.item.done").value(true))
                    .andExpect(jsonPath("$.data.completionRate").exists());
            assertEquals(1, jdbc.queryForObject("select count(*) from checklist_item where id = ? and done = 1",
                    Integer.class, id));
            mvc.perform(put("/api/v1/planning/workflow/checklist/" + id + "/toggle")
                            .header("Authorization", "Bearer " + user))
                    .andExpect(jsonPath("$.data.item.done").value(false));
        } finally {
            jdbc.update("delete from checklist_item where id = ?", id);
        }
        mvc.perform(post("/api/v1/planning/workflow/grading/convert")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"score\":85}"))
                .andExpect(jsonPath("$.data.score").value(85));
    }

    @Test @Order(25)
    void planningWorkflowAggregatesAreAvailable() throws Exception {
        String user = login(USER, USER_PW);
        for (String path : new String[]{
                "/api/v1/planning/workflow/overview",
                "/api/v1/planning/workflow/quality/summary",
                "/api/v1/planning/workflow/combinations/tree",
                "/api/v1/planning/workflow/majors/match?subjects=物理,化学",
                "/api/v1/planning/workflow/gap/check",
                "/api/v1/planning/workflow/schedule/grid",
                "/api/v1/planning/selection/statistics",
                "/api/v1/planning/schedule",
                "/api/v1/planning/compare",
        }) {
            assertEquals(200, getJson(path, user, 200).path("code").asInt(), "端点应可用: " + path);
        }
        mvc.perform(post("/api/v1/planning/workflow/gpa/whatif")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetGpa\":3.8,\"adjustments\":[]}"))
                .andExpect(jsonPath("$.data.currentGpa").exists())
                .andExpect(jsonPath("$.data.simulatedGpa").exists());
    }

    private long createReviewPlan(String token, String title) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subject", "集成测试"); body.put("title", title);
        body.put("content", "批次3 集成测试用"); body.put("targetDate", "2026-12-31");
        body.put("progress", 0); body.put("status", "未开始");
        MvcResult r = mvc.perform(post("/api/v1/planning/r/review-plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(jsonPath("$.code").value(200)).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private Map<String, Object> question(String subject, String kp, String title, String answer) {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("subject", subject); q.put("knowledgePoint", kp);
        q.put("title", title); q.put("content", "题干 " + title);
        q.put("optionsJson", List.of("A. 对", "B. 错"));
        q.put("answer", answer); q.put("explanation", "解析 " + title);
        q.put("difficulty", "中等"); q.put("source", "集成测试");
        return q;
    }

    // ============================================================ 批次4：档案与社区

    @Test @Order(26)
    void draftsAreHiddenFromPublicEndpoints() throws Exception {
        String user = login(USER, USER_PW);
        String admin = login(ADMIN, ADMIN_PW);
        long id = createArticle(user, "BATCH4_DRAFT_SECRET", "DRAFT", "BATCH4_SECRET_TAG", "BATCH4_SECRET_CAT");
        try {
            assertEquals(0, anonymousGet("/api/v1/community/contents/search?keyword=BATCH4_DRAFT_SECRET")
                    .path("data").size(), "公开检索不得泄露草稿");
            mvc.perform(get("/api/v1/community/contents/" + id + "/detail"))
                    .andExpect(jsonPath("$.code").value(404));
            assertTrue(getJson("/api/v1/community/workflow/my-contents", user, 200)
                    .path("data").path("items").toString().contains("BATCH4_DRAFT_SECRET"), "作者应能看到自己的草稿");
            assertTrue(getJson("/api/v1/admin/contents?status=DRAFT", admin, 200)
                    .path("data").path("items").toString().contains("BATCH4_DRAFT_SECRET"), "管理员应能核查草稿");
        } finally { cleanupArticle(id); }
    }

    @Test @Order(27)
    void contentReviewWorkflowPublishesArticle() throws Exception {
        String user = login(USER, USER_PW);
        String admin = login(ADMIN, ADMIN_PW);
        long id = createArticle(user, "BATCH4_APPROVED_ARTICLE", "PENDING", "复盘,批次四", "复盘");
        try {
            assertTrue(getJson("/api/v1/admin/contents/pending", admin, 200)
                    .path("data").path("items").toString().contains("BATCH4_APPROVED_ARTICLE"));
            mvc.perform(put("/api/v1/admin/contents/" + id + "/review")
                            .header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"APPROVE\"}"))
                    .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
            assertEquals("PUBLISHED", jdbc.queryForObject("select status from content where id = ?", String.class, id));
            JsonNode detail = anonymousGet("/api/v1/community/contents/" + id + "/detail").path("data");
            assertEquals("BATCH4_APPROVED_ARTICLE", detail.path("title").asText());
            assertTrue(detail.path("viewCount").asInt() >= 1, "公开详情应累加浏览量");
            Integer audits = jdbc.queryForObject(
                    "select count(*) from audit_log where action = 'REVIEW_CONTENT_APPROVE' and target_id = ?",
                    Integer.class, String.valueOf(id));
            assertTrue(audits != null && audits >= 1, "审核应写入审计日志");
        } finally { cleanupArticle(id); }
    }

    @Test @Order(28)
    void rejectionShowsReasonAndCanBeResubmitted() throws Exception {
        String user = login(USER, USER_PW);
        String admin = login(ADMIN, ADMIN_PW);
        long id = createArticle(user, "BATCH4_REJECTED_ARTICLE", "PENDING", "草稿", "随笔");
        try {
            String review = "/api/v1/admin/contents/" + id + "/review";
            mvc.perform(put(review).header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"REJECT\"}"))
                    .andExpect(jsonPath("$.code").value(400));
            mvc.perform(put(review).header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"action\":\"REJECT\",\"note\":\"标题不够具体\"}"))
                    .andExpect(jsonPath("$.data.status").value("REJECTED"));
            boolean found = false;
            for (JsonNode n : getJson("/api/v1/community/workflow/my-contents", user, 200).path("data").path("items")) {
                if (n.path("id").asLong() == id) {
                    assertTrue(n.path("reviewNote").asText().contains("标题不够具体"), "作者应看到驳回理由");
                    found = true;
                }
            }
            assertTrue(found, "我的投稿应包含被驳回稿件");
            mvc.perform(post("/api/v1/community/workflow/contents/" + id + "/submit")
                            .header("Authorization", "Bearer " + user))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
            assertEquals("", jdbc.queryForObject("select review_note from content where id = ?", String.class, id));
            mvc.perform(post("/api/v1/community/workflow/contents/" + id + "/withdraw")
                            .header("Authorization", "Bearer " + user))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"));
        } finally { cleanupArticle(id); }
    }

    @Test @Order(29)
    void interactionsRequireLoginAndAreIdempotent() throws Exception {
        String user = login(USER, USER_PW);
        String admin = login(ADMIN, ADMIN_PW);
        long published = createArticle(user, "BATCH4_INTERACTION", "PENDING", "复盘", "复盘");
        long draft = createArticle(user, "BATCH4_DRAFT_COMMENT", "DRAFT", "草稿", "随笔");
        approveArticle(admin, published);
        try {
            mvc.perform(post("/api/v1/community/contents/" + published + "/like"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(post("/api/v1/community/contents/" + published + "/like")
                            .header("Authorization", "Bearer " + user))
                    .andExpect(jsonPath("$.data.changed").value(true));
            mvc.perform(post("/api/v1/community/contents/" + published + "/like")
                            .header("Authorization", "Bearer " + user))
                    .andExpect(jsonPath("$.data.changed").value(false));
            assertEquals(1, jdbc.queryForObject("select like_count from content where id = ?", Integer.class, published));

            mvc.perform(post("/api/v1/community/workflow/contents/" + draft + "/comments")
                            .header("Authorization", "Bearer " + user)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"不应成功\"}"))
                    .andExpect(jsonPath("$.code").value(400));
            MvcResult created = mvc.perform(post("/api/v1/community/workflow/contents/" + published + "/comments")
                            .header("Authorization", "Bearer " + user)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"批次四评论\"}"))
                    .andExpect(jsonPath("$.code").value(200)).andReturn();
            long cid = json.readTree(created.getResponse().getContentAsString()).path("data").path("id").asLong();
            assertTrue(anonymousGet("/api/v1/community/contents/" + published + "/detail")
                    .path("data").path("comments").toString().contains("批次四评论"));
            mvc.perform(delete("/api/v1/community/workflow/comments/" + cid)
                            .header("Authorization", "Bearer " + user))
                    .andExpect(jsonPath("$.data.deleted").value(true));
        } finally { cleanupArticle(published); cleanupArticle(draft); }
    }

    @Test @Order(30)
    void archiveAttachmentsAndWorkflowsWork() throws Exception {
        String user = login(USER, USER_PW);
        long certId = createCertificate(user, "BATCH4_CERT");
        long rootId = 0, childId = 0, taskId = 0;
        String fileId = null;
        try {
            MockMultipartFile file = new MockMultipartFile("file", "proof.pdf", "application/pdf", "BATCH4".getBytes());
            MvcResult up = mvc.perform(multipart("/api/v1/files/file").file(file)
                            .header("Authorization", "Bearer " + user))
                    .andExpect(status().isOk()).andReturn();
            String url = json.readTree(up.getResponse().getContentAsString()).path("data").path("url").asText();
            assertTrue(url.startsWith("/files/"), "上传应返回本服务静态资源 URL");
            fileId = url.substring(url.lastIndexOf('/') + 1);

            mvc.perform(put("/api/v1/archive/workflow/attachments/bind")
                            .header("Authorization", "Bearer " + user)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("resource", "certificates", "id", certId, "url", url))))
                    .andExpect(jsonPath("$.code").value(200));
            assertEquals(url, jdbc.queryForObject("select file_url from certificate where id = ?", String.class, certId));
            mvc.perform(put("/api/v1/archive/workflow/attachments/bind")
                            .header("Authorization", "Bearer " + user)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("resource", "certificates", "id", certId, "url", "https://evil.example/x"))))
                    .andExpect(jsonPath("$.code").value(400));

            rootId = createCourseNode(user, "{\"name\":\"BATCH4_ROOT\",\"nodeType\":\"分类\"}");
            childId = createCourseNode(user, "{\"name\":\"BATCH4_CHILD\",\"parentId\":" + rootId + "}");
            mvc.perform(delete("/api/v1/archive/workflow/course-tree/" + rootId)
                            .header("Authorization", "Bearer " + user))
                    .andExpect(jsonPath("$.data.promotedChildren").value(1));
            Number parent = jdbc.queryForObject("select parent_id from course_tree_node where id = ?", Number.class, childId);
            assertEquals(0L, parent.longValue(), "删除父节点后子节点应提升为根");

            MvcResult task = mvc.perform(post("/api/v1/archive/r/thesis-board")
                            .header("Authorization", "Bearer " + user)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"BATCH4_TASK\",\"stage\":\"选题\",\"status\":\"待办\"}"))
                    .andExpect(jsonPath("$.code").value(200)).andReturn();
            taskId = json.readTree(task.getResponse().getContentAsString()).path("data").path("id").asLong();
            mvc.perform(put("/api/v1/archive/workflow/thesis/" + taskId + "/stage")
                            .header("Authorization", "Bearer " + user)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"next\":true}"))
                    .andExpect(jsonPath("$.data.stage").value("开题"));
            assertEquals(200, getJson("/api/v1/archive/workflow/elective/summary", user, 200).path("code").asInt());
        } finally {
            if (fileId != null) {
                try { mvc.perform(delete("/api/v1/files/" + fileId).header("Authorization", "Bearer " + user)); }
                catch (Exception ignored) { /* 清理失败不影响主断言 */ }
            }
            if (rootId != 0) jdbc.update("delete from course_tree_node where id = ?", rootId);
            if (childId != 0) jdbc.update("delete from course_tree_node where id = ?", childId);
            if (taskId != 0) jdbc.update("delete from thesis_board_item where id = ?", taskId);
            jdbc.update("delete from certificate where id = ?", certId);
        }
    }

    @Test @Order(31)
    void publicAggregatesAndArchiveFeaturesAreAccountScoped() throws Exception {
        String user = login(USER, USER_PW);
        String admin = login(ADMIN, ADMIN_PW);
        long draft = createArticle(user, "BATCH4_HIDDEN_GRAPH", "DRAFT", "BATCH4_SECRET_TAG", "BATCH4_SECRET_CAT");
        long published = createArticle(user, "BATCH4_SHOWN_GRAPH", "PENDING", "BATCH4_PUBLIC_TAG", "BATCH4_PUBLIC_CAT");
        approveArticle(admin, published);
        long adminCert = createCertificate(admin, "BATCH4_ADMIN_ONLY_CERT");
        try {
            JsonNode tags = anonymousGet("/api/v1/community/tag-cloud").path("data");
            assertFalse(tags.toString().contains("BATCH4_SECRET_TAG"), "标签云不得泄露草稿标签");
            assertTrue(tags.toString().contains("BATCH4_PUBLIC_TAG"));
            JsonNode graph = anonymousGet("/api/v1/community/knowledge-graph").path("data");
            assertFalse(graph.toString().contains("BATCH4_HIDDEN_GRAPH"));
            assertTrue(graph.toString().contains("BATCH4_SHOWN_GRAPH"));
            assertFalse(anonymousGet("/api/v1/community/category-stats").path("data").toString()
                    .contains("BATCH4_SECRET_CAT"));

            assertFalse(getJson("/api/v1/archive/r/certificates", user, 200).path("data").toString()
                    .contains("BATCH4_ADMIN_ONLY_CERT"), "档案列表不得跨账号");
            mvc.perform(put("/api/v1/archive/r/certificates/" + adminCert)
                            .header("Authorization", "Bearer " + user)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"篡改\"}"))
                    .andExpect(jsonPath("$.code").value(403));
            assertFalse(getJson("/api/v1/archive/r/certificates?all=true", user, 200).path("data").toString()
                    .contains("BATCH4_ADMIN_ONLY_CERT"), "普通用户 all=true 也必须按账号隔离");
        } finally {
            cleanupArticle(draft); cleanupArticle(published);
            jdbc.update("delete from certificate where id = ?", adminCert);
        }
    }

    private JsonNode anonymousGet(String path) throws Exception {
        MvcResult r = mvc.perform(get(path)).andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    private long createArticle(String token, String title, String status, String tags, String category) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title); body.put("body", "正文 " + title);
        body.put("tags", tags); body.put("category", category); body.put("status", status);
        MvcResult r = mvc.perform(post("/api/v1/community/workflow/contents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(jsonPath("$.code").value(200)).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private void approveArticle(String admin, long id) throws Exception {
        mvc.perform(put("/api/v1/admin/contents/" + id + "/review")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"APPROVE\"}"))
                .andExpect(jsonPath("$.code").value(200));
    }

    private void cleanupArticle(long id) {
        jdbc.update("delete from content_comment where content_id = ?", id);
        jdbc.update("delete from like_record where content_id = ?", id);
        jdbc.update("delete from favorite_record where content_id = ?", id);
        jdbc.update("delete from content where id = ?", id);
    }

    private long createCertificate(String token, String name) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/archive/r/certificates")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", name, "issuer", "集成测试"))))
                .andExpect(jsonPath("$.code").value(200)).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private long createCourseNode(String token, String payload) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/archive/workflow/course-tree")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(jsonPath("$.code").value(200)).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).path("data").path("id").asLong();
    }
}
