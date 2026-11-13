package com.chroniclesoftime;

import com.chroniclesoftime.shared.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 公开只读分享视图：GET /api/v1/timeline/share/{token}。
 * 该前缀已在 AuthInterceptor 的公开 GET 白名单内，无需登录；
 * token 本身即访问凭证，服务端额外校验撤销状态与有效期，并严格按分享者 user_id 取数。
 * 只返回汇总与节点，绝不返回账号信息、user_id 或任何写接口。
 */
@RestController
@RequestMapping("/api/v1/timeline/share")
public class ShareViewController {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;

    public ShareViewController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/{token}")
    public Result<?> view(@PathVariable String token) {
        if (token == null || token.isBlank() || token.length() > 64) {
            return Result.fail(404, "分享链接不存在或已失效");
        }
        List<Map<String, Object>> links = jdbc.queryForList(
                "select id, user_id, title, expires_at, revoked, view_count, created_at from share_link where token = ?", token);
        if (links.isEmpty()) return Result.fail(404, "分享链接不存在或已失效");
        Map<String, Object> link = links.get(0);
        String now = LocalDateTime.now().format(TS);
        if (num(link.get("revoked"), 0) == 1) return Result.fail(404, "该分享链接已被撤销");
        String expiresAt = text(link.get("expires_at"));
        if (!expiresAt.isBlank() && expiresAt.compareTo(now) < 0) return Result.fail(404, "该分享链接已过期");
        Object rawUid = link.get("user_id");
        if (!(rawUid instanceof Number)) return Result.fail(404, "分享链接无效");
        long uid = ((Number) rawUid).longValue();

        int views = (int) num(link.get("view_count"), 0) + 1;
        jdbc.update("update share_link set view_count = ?, last_viewed_at = ? where id = ?",
                views, now, link.get("id"));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("records", count("select count(*) from life_record where user_id = ?", uid));
        stats.put("milestones", count("select count(*) from milestone where user_id = ?", uid));
        stats.put("links", count("select count(*) from growth_link where user_id = ?", uid));
        stats.put("studyHours", round(sum("select coalesce(sum(study_hours),0) from life_record where user_id = ?", uid)));
        stats.put("reviewPlans", count("select count(*) from review_plan where user_id = ?", uid));
        stats.put("reviewDone", count("select count(*) from review_plan where user_id = ? and (progress >= 100 or status = '已完成')", uid));
        stats.put("checklistTotal", count("select count(*) from checklist_item where user_id = ?", uid));
        stats.put("checklistDone", count("select count(*) from checklist_item where user_id = ? and done = 1", uid));
        stats.put("answers", count("select count(*) from answer_records where user_id = ?", uid));
        stats.put("correctAnswers", count("select count(*) from answer_records where user_id = ? and correct = 1", uid));
        stats.put("courseCredits", round(sum("select coalesce(sum(credit),0) from term_course where user_id = ?", uid)));
        int answers = (int) stats.get("answers");
        int correct = (int) stats.get("correctAnswers");
        stats.put("correctRate", answers == 0 ? 0 : round(correct * 100.0 / answers));

        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList(
                "select record_date as occurred_at, title, category, content, is_milestone as milestone_flag "
                        + "from life_record where user_id = ? order by record_date desc limit 40", uid)) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("occurredAt", text(r.get("occurred_at")));
            n.put("title", text(r.get("title")));
            n.put("category", text(r.get("category")));
            n.put("content", clip(text(r.get("content")), 160));
            n.put("type", num(r.get("milestone_flag"), 0) == 1 ? "里程碑记录" : "成长记录");
            nodes.add(n);
        }
        List<Map<String, Object>> milestones = new ArrayList<>();
        for (Map<String, Object> m : jdbc.queryForList(
                "select occurred_at, title, description, icon, color from milestone where user_id = ? "
                        + "order by occurred_at desc limit 12", uid)) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("occurredAt", text(m.get("occurred_at")));
            n.put("title", text(m.get("title")));
            n.put("content", clip(text(m.get("description")), 120));
            n.put("icon", text(m.get("icon")).isBlank() ? "star" : text(m.get("icon")));
            n.put("color", text(m.get("color")).isBlank() ? "mint" : text(m.get("color")));
            n.put("type", "里程碑");
            milestones.add(n);
            nodes.add(n);
        }
        nodes.sort((a, b) -> text(b.get("occurredAt")).compareTo(text(a.get("occurredAt"))));
        if (nodes.size() > 40) nodes = new ArrayList<>(nodes.subList(0, 40));

        List<Map<String, Object>> progress = new ArrayList<>();
        for (Map<String, Object> p : jdbc.queryForList(
                "select subject, title, progress, status, target_date from review_plan where user_id = ? "
                        + "order by target_date limit 20", uid)) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("subject", text(p.get("subject")));
            n.put("title", text(p.get("title")));
            n.put("progress", (int) num(p.get("progress"), 0));
            n.put("status", text(p.get("status")));
            n.put("targetDate", text(p.get("target_date")));
            progress.add(n);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("title", text(link.get("title")).isBlank() ? "我的拾光时间线" : text(link.get("title")));
        out.put("createdAt", text(link.get("created_at")));
        out.put("expiresAt", expiresAt);
        out.put("viewCount", views);
        out.put("stats", stats);
        out.put("timeline", nodes);
        out.put("milestones", milestones);
        out.put("reviewProgress", progress);
        return Result.ok(out);
    }

    private int count(String sql, long uid) {
        Integer n = jdbc.queryForObject(sql, Integer.class, uid);
        return n == null ? 0 : n;
    }
    private double sum(String sql, long uid) {
        Double n = jdbc.queryForObject(sql, Double.class, uid);
        return n == null ? 0 : n;
    }
    private static double round(double v) { return Math.round(v * 100) / 100.0; }
    private static String clip(String s, int max) { return s.length() <= max ? s : s.substring(0, max) + "…"; }
    private static String text(Object v) { return v == null ? "" : String.valueOf(v); }
    private static double num(Object v, double fallback) {
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof Boolean b) return b ? 1 : 0;
        try { return Double.parseDouble(String.valueOf(v)); } catch (RuntimeException e) { return fallback; }
    }
}
