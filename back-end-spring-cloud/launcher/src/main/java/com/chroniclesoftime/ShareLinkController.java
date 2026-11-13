package com.chroniclesoftime;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.shared.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 只读分享链接管理：生成、列表、撤销、续期、删除（需登录，且只能操作自己的链接）。 */
@RestController
@RequestMapping("/api/v1/planning/share-links")
public class ShareLinkController {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbc;

    public ShareLinkController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public Result<?> list() {
        Long uid = AuthContext.userId();
        if (uid == null) return Result.fail(401, "请先登录");
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList(
                "select id, user_id, token, title, expires_at, revoked, view_count, last_viewed_at, created_at "
                        + "from share_link where user_id = ? order by id desc", uid)) {
            out.add(toDto(row));
        }
        return Result.ok(out);
    }

    /** body: { title?: string, days?: number } —— days 默认 7，范围 1~365。 */
    @PostMapping
    public Result<?> create(@RequestBody(required = false) Map<String, Object> body) {
        Long uid = AuthContext.userId();
        if (uid == null) return Result.fail(401, "请先登录");
        Map<String, Object> b = body == null ? Map.of() : body;
        String title = text(b.get("title")).isBlank() ? "我的拾光时间线" : text(b.get("title"));
        int days = (int) Math.max(1, Math.min(365, num(b.get("days"), 7)));
        LocalDateTime now = LocalDateTime.now();
        String token = newToken();
        String createdAt = now.format(TS);
        String expiresAt = now.plusDays(days).format(TS);
        jdbc.update("insert into share_link (user_id, token, title, expires_at, revoked, view_count, created_at) "
                + "values (?, ?, ?, ?, 0, 0, ?)", uid, token, title, expiresAt, createdAt);
        Long id = jdbc.queryForObject("select id from share_link where token = ?", Long.class, token);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", id); out.put("title", title); out.put("token", token);
        out.put("url", "/share/" + token); out.put("expiresAt", expiresAt);
        out.put("createdAt", createdAt); out.put("revoked", false);
        out.put("expired", false); out.put("viewCount", 0); out.put("days", days);
        return Result.ok(out);
    }

    @PutMapping("/{id}/revoke")
    public Result<?> revoke(@PathVariable Long id) {
        if (!owned(id)) return Result.fail(403, "无权操作该分享链接");
        jdbc.update("update share_link set revoked = 1 where id = ?", id);
        return Result.ok(Map.of("id", id, "revoked", true));
    }

    /** 续期：重新计算有效期，并可顺带解除撤销（用于「重新启用」）。 */
    @PutMapping("/{id}/renew")
    public Result<?> renew(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        if (!owned(id)) return Result.fail(403, "无权操作该分享链接");
        Map<String, Object> b = body == null ? Map.of() : body;
        int days = (int) Math.max(1, Math.min(365, num(b.get("days"), 7)));
        String expiresAt = LocalDateTime.now().plusDays(days).format(TS);
        jdbc.update("update share_link set expires_at = ?, revoked = 0 where id = ?", expiresAt, id);
        return Result.ok(Map.of("id", id, "expiresAt", expiresAt, "revoked", false, "days", days));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        if (!owned(id)) return Result.fail(403, "无权操作该分享链接");
        jdbc.update("delete from share_link where id = ?", id);
        return Result.ok(Map.of("id", id, "deleted", true));
    }

    private boolean owned(Long id) {
        Long uid = AuthContext.userId();
        if (uid == null) return false;
        if (AuthContext.isAdmin()) return true;
        List<Long> owners = jdbc.queryForList("select user_id from share_link where id = ?", Long.class, id);
        return !owners.isEmpty() && uid.equals(owners.get(0));
    }

    private Map<String, Object> toDto(Map<String, Object> row) {
        Map<String, Object> out = new LinkedHashMap<>();
        String token = text(row.get("token"));
        String expiresAt = text(row.get("expires_at"));
        String now = LocalDateTime.now().format(TS);
        out.put("id", row.get("id"));
        out.put("title", text(row.get("title")).isBlank() ? "我的拾光时间线" : text(row.get("title")));
        out.put("token", token);
        out.put("url", "/share/" + token);
        out.put("expiresAt", expiresAt);
        out.put("revoked", num(row.get("revoked"), 0) == 1);
        out.put("expired", !expiresAt.isBlank() && expiresAt.compareTo(now) < 0);
        out.put("viewCount", (int) num(row.get("view_count"), 0));
        out.put("lastViewedAt", text(row.get("last_viewed_at")));
        out.put("createdAt", text(row.get("created_at")));
        return out;
    }

    private static String newToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private static String text(Object v) { return v == null ? "" : String.valueOf(v); }
    private static double num(Object v, double fallback) {
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (RuntimeException e) { return fallback; }
    }
}
