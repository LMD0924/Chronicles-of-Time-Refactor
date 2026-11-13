package com.chroniclesoftime.identity.interfaces;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.auth.PasswordHasher;
import com.chroniclesoftime.identity.application.IdentityApplicationService;
import com.chroniclesoftime.identity.domain.model.Account;
import com.chroniclesoftime.identity.domain.model.AuditLog;
import com.chroniclesoftime.identity.domain.repository.AccountRepository;
import com.chroniclesoftime.identity.domain.repository.AuditLogRepository;
import com.chroniclesoftime.shared.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 管理后台：用户与角色、内容治理、字典、业务核查、审计日志、系统统计（所有写操作留痕）。
 *  跨表只读核查与字典维护通过共享 DataSource 的 JdbcTemplate 执行原生 SQL：
 *  表名与列名全部来自代码内白名单，用户输入只作为参数绑定，避免 SQL 注入与跨模块依赖。 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AccountRepository accounts;
    private final AuditLogRepository audits;
    private final IdentityApplicationService service;
    private final JdbcTemplate jdbc;

    public AdminController(AccountRepository accounts, AuditLogRepository audits,
                           IdentityApplicationService service, JdbcTemplate jdbc) {
        this.accounts = accounts; this.audits = audits; this.service = service; this.jdbc = jdbc;
    }

    /** 概览计数涉及的业务表（白名单，表名绝不取自请求参数）。 */
    private static final List<String> COUNT_TABLES = List.of(
            "account", "life_record", "milestone", "question_bank", "score_record",
            "content", "content_comment", "volunteer_plan", "volunteer_detail",
            "admission_simulation", "course_selection", "selection_intention",
            "audit_log", "user_profile", "subject_option", "grading_scale", "major_requirement"
    );

    /** 字典 type（URL 段）→ 资源表名。 */
    private static final Map<String, String> DICT_TABLE = Map.of(
            "subject-option", "subject_option",
            "grading-scale", "grading_scale",
            "major-requirement", "major_requirement"
    );

    /** 各字典可写列白名单（数据库列名；请求体按驼峰 key 读取）。 */
    private static final Map<String, List<String>> DICT_COLUMNS = Map.of(
            "subject-option", List.of("name", "category", "is_first", "is_required", "description"),
            "grading-scale", List.of("subject_id", "level", "min_score", "max_score", "ratio", "active"),
            "major-requirement", List.of("major_code", "major_name", "category", "level", "required_subjects", "description")
    );
    private static final Set<String> BOOL_COLUMNS = Set.of("is_first", "is_required", "active");
    private static final Set<String> INT_COLUMNS = Set.of("subject_id");
    private static final Set<String> DEC_COLUMNS = Set.of("min_score", "max_score", "ratio");

    private static final int MAX_LIMIT = 200;
    private static final int TOP_N = 5;

    // ============================================================ 现有端点（保留兼容）

    @GetMapping("/users")
    public Result<?> users(@RequestParam(required = false) String keyword) {
        List<Account> list = accounts.findAll();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            list = list.stream().filter(a -> (a.getUsername() + a.getNickname()).toLowerCase().contains(kw)).toList();
        }
        return Result.ok(list);
    }

    @PostMapping("/users")
    public Result<?> createUser(@RequestBody Map<String, Object> body) {
        String username = String.valueOf(body.get("username"));
        if (!accounts.findBy("username", username).isEmpty()) return Result.fail(409, "账号已存在");
        Account a = new Account();
        a.setUsername(username);
        a.setNickname(String.valueOf(body.getOrDefault("nickname", username)));
        a.setPasswordHash(PasswordHasher.hash(String.valueOf(body.getOrDefault("password", "Cot@2026new"))));
        a.setRole(String.valueOf(body.getOrDefault("role", "USER")));
        a.setStatus("ACTIVE"); a.setTokenVersion(1); a.setLoginFailCount(0); a.setCreatedAt(now());
        Account saved = accounts.save(a);
        audit("CREATE_USER", "account", String.valueOf(saved.getId()), "", saved.getRole());
        return Result.ok(saved);
    }

    @PutMapping("/users/{id}")
    public Result<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Account a = accounts.findById(id);
        if (a == null) return Result.fail(404, "账号不存在");
        String before = a.getRole() + "/" + a.getStatus();
        if (body.containsKey("role")) a.setRole(String.valueOf(body.get("role")));
        if (body.containsKey("status")) a.setStatus(String.valueOf(body.get("status")));
        if (body.containsKey("nickname")) a.setNickname(String.valueOf(body.get("nickname")));
        accounts.save(a);
        audit("UPDATE_USER", "account", String.valueOf(id), before, a.getRole() + "/" + a.getStatus());
        return Result.ok(a);
    }

    @PostMapping("/users/{id}/reset-password")
    public Result<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Account a = accounts.findById(id);
        if (a == null) return Result.fail(404, "账号不存在");
        a.setPasswordHash(PasswordHasher.hash(String.valueOf(body.getOrDefault("password", "Cot@2026reset"))));
        a.setTokenVersion((a.getTokenVersion() == null ? 1 : a.getTokenVersion()) + 1);
        a.setLoginFailCount(0); a.setLockedUntil(null);
        accounts.save(a);
        audit("RESET_PASSWORD", "account", String.valueOf(id), "", "token revoked");
        return Result.ok(Map.of("reset", true, "reloginRequired", true));
    }

    @GetMapping("/audit")
    public Result<?> auditLog() { return Result.ok(audits.findAll()); }

    @GetMapping("/stats")
    public Result<?> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("accounts", accounts.count());
        m.put("auditLogs", audits.count());
        m.put("identity", service.stats());
        return Result.ok(m);
    }

    // ============================================================ 后台概览

    /** 各业务表计数 + 用户/管理员数 + 最近审计 / 志愿方案 / 选科申请，全部真实聚合。 */
    @GetMapping("/overview")
    public Result<?> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> counts = new LinkedHashMap<>();
        for (String table : COUNT_TABLES) counts.put(table, count("SELECT COUNT(*) FROM " + table));
        data.put("counts", counts);
        data.put("userCount", count("SELECT COUNT(*) FROM account WHERE role = ?", "USER"));
        data.put("adminCount", count("SELECT COUNT(*) FROM account WHERE role = ?", "ADMIN"));
        data.put("recentAudit", top(query(
                "SELECT id, operator_id, operator_name, action, target_type, target_id, "
                        + "before_value, after_value, created_at FROM audit_log ORDER BY id DESC"), TOP_N));
        data.put("recentPlans", top(query(
                "SELECT p.id, p.user_id, p.title, p.plan_year, p.province, p.status, p.created_at, "
                        + "(SELECT COUNT(*) FROM volunteer_detail d WHERE d.plan_id = p.id) AS detail_count "
                        + "FROM volunteer_plan p ORDER BY p.id DESC"), TOP_N));
        data.put("recentSelections", top(query(
                "SELECT id, user_id, combination_id, status, confirmed_at, note, created_at "
                        + "FROM course_selection ORDER BY id DESC"), TOP_N));
        return Result.ok(data);
    }

    // ============================================================ 用户状态 / 角色

    /** 启用 / 停用：更新状态、token_version +1 使旧令牌失效；启用时清零登录失败计数。 */
    @PostMapping("/users/{id}/status")
    public Result<?> setUserStatus(@PathVariable Long id, @RequestParam String status) {
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status))
            return Result.fail(400, "status 只能是 ACTIVE 或 DISABLED");
        Account a = accounts.findById(id);
        if (a == null) return Result.fail(404, "账号不存在");
        if ("DISABLED".equals(status) && id.equals(AuthContext.userId()))
            return Result.fail(400, "不能停用当前登录的管理员账号");
        String before = a.getStatus();
        a.setStatus(status);
        a.setTokenVersion((a.getTokenVersion() == null ? 1 : a.getTokenVersion()) + 1);
        if ("ACTIVE".equals(status)) { a.setLoginFailCount(0); a.setLockedUntil(null); }
        accounts.save(a);
        audit("UPDATE_USER_STATUS", "account", String.valueOf(id), before, status + " / token_version +1");
        return Result.ok(Map.of("id", id, "status", status, "tokenRevoked", true));
    }

    /** 修改已有账号角色（USER / ADMIN），禁止将自己的管理员权限降权。 */
    @PutMapping("/users/{id}/role")
    public Result<?> setUserRole(@PathVariable Long id, @RequestParam String role) {
        if (!"USER".equals(role) && !"ADMIN".equals(role))
            return Result.fail(400, "role 只能是 USER 或 ADMIN");
        Account a = accounts.findById(id);
        if (a == null) return Result.fail(404, "账号不存在");
        if (id.equals(AuthContext.userId()) && "ADMIN".equals(a.getRole()) && !"ADMIN".equals(role))
            return Result.fail(400, "不能将自己的管理员权限降权");
        String before = a.getRole();
        a.setRole(role);
        accounts.save(a);
        audit("UPDATE_USER_ROLE", "account", String.valueOf(id), before, role);
        return Result.ok(Map.of("id", id, "role", role));
    }

    // ============================================================ 审计 / 业务核查（只读）

    @GetMapping("/audit-logs")
    public Result<?> auditLogs(@RequestParam(defaultValue = "50") int limit,
                               @RequestParam(required = false) String action) {
        String sql = "SELECT id, operator_id, operator_name, action, target_type, target_id, "
                + "before_value, after_value, created_at FROM audit_log";
        List<Map<String, Object>> rows = (action != null && !action.isBlank())
                ? query(sql + " WHERE action = ? ORDER BY id DESC", action)
                : query(sql + " ORDER BY id DESC");
        List<Map<String, Object>> items = top(rows, limit(limit));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("total", items.size());
        return Result.ok(data);
    }

    /** 跨账号志愿方案只读核查（含明细数）。 */
    @GetMapping("/plans")
    public Result<?> plans(@RequestParam(defaultValue = "20") int limit) {
        List<Map<String, Object>> rows = query(
                "SELECT p.id, p.user_id, p.title, p.plan_year, p.province, p.status, p.created_at, "
                        + "(SELECT COUNT(*) FROM volunteer_detail d WHERE d.plan_id = p.id) AS detail_count "
                        + "FROM volunteer_plan p ORDER BY p.id DESC");
        return Result.ok(Map.of("items", top(rows, limit(limit))));
    }

    /** 跨账号模拟结果只读核查。 */
    @GetMapping("/simulations")
    public Result<?> simulations(@RequestParam(defaultValue = "20") int limit) {
        List<Map<String, Object>> rows = query(
                "SELECT id, user_id, plan_id, detail_id, result, probability, analysis, created_at "
                        + "FROM admission_simulation ORDER BY id DESC");
        return Result.ok(Map.of("items", top(rows, limit(limit))));
    }

    // ============================================================ 内容治理

    @GetMapping("/contents")
    public Result<?> contents(@RequestParam(required = false) String status,
                              @RequestParam(required = false) String keyword) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, user_id, author_name, title, category, status, view_count, like_count, "
                        + "favorite_count, review_note, reviewed_by, reviewed_at, created_at, updated_at FROM content WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (status != null && !status.isBlank()) { sql.append(" AND status = ?"); args.add(status); }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (title LIKE ? OR author_name LIKE ?)");
            String kw = "%" + keyword + "%";
            args.add(kw); args.add(kw);
        }
        sql.append(" ORDER BY id DESC");
        return Result.ok(Map.of("items", query(sql.toString(), args.toArray())));
    }

    /** 待审核队列：按提交时间倒序，附带正文摘要便于管理员判断。 */
    @GetMapping("/contents/pending")
    public Result<?> pendingContents() {
        return Result.ok(Map.of("items", query(
                "SELECT id, user_id, author_name, title, category, tags, summary, created_at, updated_at "
                        + "FROM content WHERE status = 'PENDING' ORDER BY updated_at DESC, id DESC")));
    }

    /** D4 审核：通过 → PUBLISHED；驳回 → REJECTED 且必须写明理由（作者可见）。 */
    @PutMapping("/contents/{id}/review")
    public Result<?> reviewContent(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String action = String.valueOf(body.getOrDefault("action", "")).toUpperCase(Locale.ROOT);
        String note = String.valueOf(body.getOrDefault("note", "")).trim();
        if (!"APPROVE".equals(action) && !"REJECT".equals(action)) {
            return Result.fail(400, "action 只能是 APPROVE / REJECT");
        }
        if ("REJECT".equals(action) && note.isBlank()) return Result.fail(400, "驳回必须填写理由");
        if (note.length() > 1000) return Result.fail(400, "审核意见最多 1000 字");
        List<Map<String, Object>> cur = query("SELECT status, title FROM content WHERE id = ?", id);
        if (cur.isEmpty()) return Result.fail(404, "文章不存在");
        String before = String.valueOf(cur.get(0).get("status"));
        if (!"PENDING".equals(before)) return Result.fail(400, "只有待审核的稿件可以审核");

        String after = "APPROVE".equals(action) ? "PUBLISHED" : "REJECTED";
        jdbc.update("UPDATE content SET status = ?, review_note = ?, reviewed_by = ?, reviewed_at = ?, updated_at = ? WHERE id = ?",
                after, note, AuthContext.userId(), now(), now(), id);
        audit("REVIEW_CONTENT_" + action, "content", String.valueOf(id), before,
                after + (note.isBlank() ? "" : " | " + note));
        return Result.ok(Map.of("id", id, "status", after, "reviewNote", note));
    }

    /** 内容发布 / 下架 / 归档，写含前后状态的审计。 */
    @PutMapping("/contents/{id}/status")
    public Result<?> setContentStatus(@PathVariable Long id, @RequestParam String status) {
        if (!"PUBLISHED".equals(status) && !"HIDDEN".equals(status) && !"ARCHIVED".equals(status))
            return Result.fail(400, "status 只能是 PUBLISHED / HIDDEN / ARCHIVED");
        List<Map<String, Object>> cur = query("SELECT status FROM content WHERE id = ?", id);
        if (cur.isEmpty()) return Result.fail(404, "文章不存在");
        String before = String.valueOf(cur.get(0).get("status"));
        jdbc.update("UPDATE content SET status = ?, updated_at = ? WHERE id = ?", status, now(), id);
        audit("UPDATE_CONTENT_STATUS", "content", String.valueOf(id), before, status);
        return Result.ok(Map.of("id", id, "status", status));
    }

    /** 删除文章，同时清理该文章的评论 / 点赞 / 收藏。 */
    @DeleteMapping("/contents/{id}")
    public Result<?> deleteContent(@PathVariable Long id) {
        List<Map<String, Object>> cur = query("SELECT title FROM content WHERE id = ?", id);
        if (cur.isEmpty()) return Result.fail(404, "文章不存在");
        String before = String.valueOf(cur.get(0).get("title"));
        jdbc.update("DELETE FROM content_comment WHERE content_id = ?", id);
        jdbc.update("DELETE FROM like_record WHERE content_id = ?", id);
        jdbc.update("DELETE FROM favorite_record WHERE content_id = ?", id);
        jdbc.update("DELETE FROM content WHERE id = ?", id);
        audit("DELETE_CONTENT", "content", String.valueOf(id), before, "已删除文章及其评论/点赞/收藏");
        return Result.ok(Map.of("id", id, "deleted", true));
    }

    // ============================================================ 数据字典

    /** 三类字典全量分组返回，key 为资源表名。 */
    @GetMapping("/dictionaries")
    public Result<?> dictionaries() {
        Map<String, Object> data = new LinkedHashMap<>();
        DICT_TABLE.forEach((type, table) -> data.put(table, query("SELECT * FROM " + table + " ORDER BY id")));
        return Result.ok(data);
    }

    @PostMapping("/dictionaries/{type}")
    public Result<?> addDictionary(@PathVariable String type, @RequestBody Map<String, Object> body) {
        String table = DICT_TABLE.get(type);
        if (table == null) return Result.fail(400, "未知字典类型");
        List<String> use = writableColumns(type, body);
        if (use.isEmpty()) return Result.fail(400, "没有可写入的字段");
        String placeholders = String.join(",", Collections.nCopies(use.size(), "?"));
        String sql = "INSERT INTO " + table + " (" + String.join(",", use)
                + ", created_at) VALUES (" + placeholders + ", ?)";
        List<Object> args = new ArrayList<>();
        for (String c : use) args.add(norm(c, body.get(toCamel(c))));
        args.add(now());
        jdbc.update(sql, args.toArray());
        audit("CREATE_DICTIONARY", table, type, "", String.valueOf(body));
        return Result.ok(Map.of("created", true, "type", type));
    }

    @PutMapping("/dictionaries/{type}/{id}")
    public Result<?> updateDictionary(@PathVariable String type, @PathVariable Long id,
                                      @RequestBody Map<String, Object> body) {
        String table = DICT_TABLE.get(type);
        if (table == null) return Result.fail(400, "未知字典类型");
        List<String> use = writableColumns(type, body);
        if (use.isEmpty()) return Result.fail(400, "没有可更新的字段");
        List<Map<String, Object>> cur = query("SELECT * FROM " + table + " WHERE id = ?", id);
        if (cur.isEmpty()) return Result.fail(404, "字典条目不存在");
        String set = String.join(", ", use.stream().map(c -> c + " = ?").toList());
        List<Object> args = new ArrayList<>();
        for (String c : use) args.add(norm(c, body.get(toCamel(c))));
        args.add(id);
        jdbc.update("UPDATE " + table + " SET " + set + " WHERE id = ?", args.toArray());
        audit("UPDATE_DICTIONARY", table, String.valueOf(id), String.valueOf(cur.get(0)), String.valueOf(body));
        return Result.ok(Map.of("id", id, "type", type, "updated", true));
    }

    @DeleteMapping("/dictionaries/{type}/{id}")
    public Result<?> deleteDictionary(@PathVariable String type, @PathVariable Long id) {
        String table = DICT_TABLE.get(type);
        if (table == null) return Result.fail(400, "未知字典类型");
        List<Map<String, Object>> cur = query("SELECT * FROM " + table + " WHERE id = ?", id);
        if (cur.isEmpty()) return Result.fail(404, "字典条目不存在");
        jdbc.update("DELETE FROM " + table + " WHERE id = ?", id);
        audit("DELETE_DICTIONARY", table, String.valueOf(id), String.valueOf(cur.get(0)), "已删除");
        return Result.ok(Map.of("id", id, "type", type, "deleted", true));
    }

    // ============================================================ helpers

    private void audit(String action, String targetType, String targetId, String before, String after) {
        AuditLog log = new AuditLog();
        log.setOperatorId(AuthContext.userId());
        log.setOperatorName(AuthContext.username());
        log.setAction(action); log.setTargetType(targetType); log.setTargetId(targetId);
        log.setBeforeValue(before); log.setAfterValue(after); log.setCreatedAt(now());
        audits.save(log);
    }

    private static String now() { return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }

    /** 计数：只取结果集首行首个值，兼容不同驱动的数值类型。 */
    private long count(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        if (rows.isEmpty() || rows.get(0).isEmpty()) return 0L;
        Object v = rows.get(0).values().iterator().next();
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    /** 查询并把下划线列名统一转成驼峰，便于前端与实体 JSON 风格一致。 */
    private List<Map<String, Object>> query(String sql, Object... args) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList(sql, args)) {
            Map<String, Object> m = new LinkedHashMap<>();
            row.forEach((k, v) -> m.put(toCamel(k.toLowerCase(Locale.ROOT)), v));
            out.add(m);
        }
        return out;
    }

    private static String toCamel(String s) {
        StringBuilder b = new StringBuilder();
        boolean upper = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_') upper = true;
            else if (upper) { b.append(Character.toUpperCase(c)); upper = false; }
            else b.append(c);
        }
        return b.toString();
    }

    private static List<Map<String, Object>> top(List<Map<String, Object>> rows, int n) {
        return rows.stream().limit(n).toList();
    }

    private static int limit(int raw) { return Math.min(Math.max(raw, 1), MAX_LIMIT); }

    /** 仅保留白名单列中请求体确实提交了的字段（按驼峰 key 对应列名）。 */
    private static List<String> writableColumns(String type, Map<String, Object> body) {
        return DICT_COLUMNS.get(type).stream().filter(c -> body.containsKey(toCamel(c))).toList();
    }

    /** 归一化字典写入值：布尔列转 1/0，整数列转 Long，小数列转 Double，其余转字符串。 */
    private static Object norm(String col, Object v) {
        if (v == null) return null;
        if (BOOL_COLUMNS.contains(col)) {
            boolean on = v instanceof Boolean ? (Boolean) v
                    : "true".equalsIgnoreCase(String.valueOf(v)) || "1".equals(String.valueOf(v));
            return on ? 1 : 0;
        }
        if (INT_COLUMNS.contains(col)) {
            try { return Long.valueOf(String.valueOf(v)); } catch (NumberFormatException e) { return null; }
        }
        if (DEC_COLUMNS.contains(col)) {
            try { return Double.valueOf(String.valueOf(v)); } catch (NumberFormatException e) { return null; }
        }
        return String.valueOf(v);
    }
}
