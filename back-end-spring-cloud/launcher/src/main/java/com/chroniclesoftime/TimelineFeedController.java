package com.chroniclesoftime;

import com.chroniclesoftime.archive.application.ArchiveApplicationService;
import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.community.application.CommunityApplicationService;
import com.chroniclesoftime.learning.application.LearningApplicationService;
import com.chroniclesoftime.planning.application.PlanningApplicationService;
import com.chroniclesoftime.shared.Result;
import com.chroniclesoftime.timeline.application.TimelineApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 跨域时间线聚合：把记录、里程碑、学习、规划、社区、档案的数据挂到同一条时间轴上，
 * 这是「不再按人生阶段切分、功能之间建立关联」的落地实现。
 *
 * 筛选：type / q / category / period / dateFrom / dateTo；/feed/page 提供分页与总数。
 */
@RestController
@RequestMapping("/api/v1/timeline")
public class TimelineFeedController {
    private final TimelineApplicationService timeline;
    private final LearningApplicationService learning;
    private final PlanningApplicationService planning;
    private final CommunityApplicationService community;
    private final ArchiveApplicationService archive;

    public TimelineFeedController(TimelineApplicationService timeline, LearningApplicationService learning,
                                  PlanningApplicationService planning, CommunityApplicationService community,
                                  ArchiveApplicationService archive) {
        this.timeline = timeline; this.learning = learning; this.planning = planning;
        this.community = community; this.archive = archive;
    }

    /**
     * 私有节点按当前账号隔离；社区文章属于公开发布内容，所有登录用户均可见。
     * 管理员显式传 all=true 时可跨账号核查（与统一资源接口的行为一致）。
     */
    @GetMapping("/feed")
    public Result<?> feed(@RequestParam(required = false) String type,
                          @RequestParam(defaultValue = "false") boolean all,
                          @RequestParam(required = false) String q,
                          @RequestParam(required = false) String category,
                          @RequestParam(required = false) String period,
                          @RequestParam(required = false) String dateFrom,
                          @RequestParam(required = false) String dateTo,
                          @RequestParam(defaultValue = "0") int limit) {
        List<Map<String, Object>> entries = build(type, all, q, category, period, dateFrom, dateTo);
        if (limit > 0 && entries.size() > limit) return Result.ok(new ArrayList<>(entries.subList(0, limit)));
        return Result.ok(entries);
    }

    /** 分页版时间线：返回 {items, total, page, size, pages}，供「加载更多」使用。 */
    @GetMapping("/feed/page")
    public Result<?> feedPage(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(required = false) String type,
                              @RequestParam(defaultValue = "false") boolean all,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) String category,
                              @RequestParam(required = false) String period,
                              @RequestParam(required = false) String dateFrom,
                              @RequestParam(required = false) String dateTo) {
        List<Map<String, Object>> entries = build(type, all, q, category, period, dateFrom, dateTo);
        int safeSize = Math.max(1, Math.min(size, 200));
        int pages = Math.max(1, (entries.size() + safeSize - 1) / safeSize);
        int safePage = Math.max(1, Math.min(page, pages));
        int from = (safePage - 1) * safeSize;
        int to = Math.min(from + safeSize, entries.size());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", from >= to ? List.of() : new ArrayList<>(entries.subList(from, to)));
        out.put("total", entries.size());
        out.put("page", safePage);
        out.put("size", safeSize);
        out.put("pages", pages);
        return Result.ok(out);
    }

    private List<Map<String, Object>> build(String type, boolean all, String q,
                                            String category, String period,
                                            String dateFrom, String dateTo) {
        Long uid = AuthContext.userId();
        boolean crossAccount = all && AuthContext.isAdmin();
        List<Map<String, Object>> entries = new ArrayList<>();
        add(entries, "记录", scoped(timeline.rows("records"), uid, crossAccount), "recordDate", "title", "content", "mint", "/timeline/records");
        add(entries, "里程碑", scoped(timeline.rows("milestones"), uid, crossAccount), "occurredAt", "title", "description", "amber", "/timeline/milestones");
        add(entries, "工作", scoped(timeline.rows("work-records"), uid, crossAccount), "startDate", "position", "content", "teal", "/timeline/work");
        add(entries, "进阶", scoped(timeline.rows("advance-records"), uid, crossAccount), "occurredAt", "title", "description", "violet", "/timeline/advances");
        add(entries, "成绩", scoped(learning.rows("scores"), uid, crossAccount), "examDate", "examName", "score", "sky", "/learning/scores");
        add(entries, "练习", scoped(learning.rows("sessions"), uid, crossAccount), "startedAt", "title", "correctCount", "mint", "/learning/practice");
        add(entries, "规划", scoped(planning.rows("review-plans"), uid, crossAccount), "targetDate", "title", "content", "sky", "/planning/review");
        add(entries, "目标", scoped(planning.rows("target-paths"), uid, crossAccount), "deadline", "targetName", "milestone", "rose", "/planning/target");
        add(entries, "文章", community.rows("contents"), "createdAt", "title", "summary", "violet", "/community");
        add(entries, "证书", scoped(archive.rows("certificates"), uid, crossAccount), "issuedAt", "name", "issuer", "amber", "/archive/certificates");
        add(entries, "论文", scoped(archive.rows("papers"), uid, crossAccount), "createdAt", "title", "abstractText", "teal", "/archive/papers");

        String kw = q == null ? "" : q.trim().toLowerCase();
        entries.removeIf(e -> {
            if (e.get("occurredAt") == null || String.valueOf(e.get("occurredAt")).isBlank()) return true;
            if (type != null && !type.isBlank() && !type.equals(e.get("type"))) return true;
            if (!kw.isEmpty() && !contains(e.get("title"), kw) && !contains(e.get("summary"), kw)) return true;
            String d = dateOf(e.get("occurredAt"));
            if (dateFrom != null && !dateFrom.isBlank() && d.compareTo(dateFrom.trim()) < 0) return true;
            if (dateTo != null && !dateTo.isBlank() && d.compareTo(dateTo.trim()) > 0) return true;
            if (category != null && !category.isBlank() && !category.equals(text(e.get("category")))) return true;
            if (period != null && !period.isBlank() && !period.equals(text(e.get("period")))) return true;
            return false;
        });
        entries.sort((a, b) -> String.valueOf(b.get("occurredAt")).compareTo(String.valueOf(a.get("occurredAt"))));
        return entries;
    }

    private static boolean contains(Object value, String kw) {
        return value != null && String.valueOf(value).toLowerCase().contains(kw);
    }
    private static String text(Object value) { return value == null ? "" : String.valueOf(value); }
    private static String dateOf(Object value) {
        String s = String.valueOf(value);
        return s.length() > 10 ? s.substring(0, 10) : s;
    }

    private List<Map<String, Object>> scoped(List<Map<String, Object>> rows, Long uid, boolean crossAccount) {
        if (crossAccount) return rows;
        if (uid == null) return List.of();
        return rows.stream().filter(r -> uid.equals(longOf(r.get("userId")))).toList();
    }

    private static Long longOf(Object o) { return o instanceof Number n ? n.longValue() : null; }

    private void add(List<Map<String, Object>> out, String type, List<Map<String, Object>> rows,
                     String dateKey, String titleKey, String summaryKey, String color, String link) {
        for (Map<String, Object> r : rows) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("id", type + "-" + r.get("id"));
            e.put("type", type);
            e.put("occurredAt", r.get(dateKey) == null ? r.get("createdAt") : r.get(dateKey));
            e.put("title", r.get(titleKey));
            e.put("summary", r.get(summaryKey));
            e.put("link", link);
            e.put("color", color);
            e.put("sourceId", r.get("id"));
            e.put("category", r.get("category"));
            e.put("period", r.get("period"));
            e.put("tags", r.get("tags"));
            out.add(e);
        }
    }
}
