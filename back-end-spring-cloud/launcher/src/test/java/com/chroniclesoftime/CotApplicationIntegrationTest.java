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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
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
}
