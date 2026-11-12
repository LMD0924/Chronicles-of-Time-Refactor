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
}
