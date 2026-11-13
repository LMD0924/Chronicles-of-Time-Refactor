package com.chroniclesoftime;

import com.chroniclesoftime.archive.application.ArchiveApplicationService;
import com.chroniclesoftime.community.application.CommunityApplicationService;
import com.chroniclesoftime.identity.application.IdentityApplicationService;
import com.chroniclesoftime.learning.application.LearningApplicationService;
import com.chroniclesoftime.planning.application.PlanningApplicationService;
import com.chroniclesoftime.shared.Result;
import com.chroniclesoftime.timeline.application.TimelineApplicationService;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;

/**
 * 横切 · 全局搜索：跨限界上下文聚合检索。
 * 私有数据一律走 rowsScoped（按账号隔离），社区文章只纳入 PUBLISHED 与本人稿件，
 * 结果只暴露标题 / 摘要 / 日期 / 跳转地址，不返回账号信息或完整正文。
 */
@RestController
@RequestMapping("/api/v1/search")
public class GlobalSearchController {
    private final TimelineApplicationService timeline;
    private final LearningApplicationService learning;
    private final PlanningApplicationService planning;
    private final ArchiveApplicationService archive;
    private final CommunityApplicationService community;
    private final IdentityApplicationService identity;

    public GlobalSearchController(TimelineApplicationService timeline, LearningApplicationService learning,
                                  PlanningApplicationService planning, ArchiveApplicationService archive,
                                  CommunityApplicationService community, IdentityApplicationService identity) {
        this.timeline = timeline; this.learning = learning; this.planning = planning;
        this.archive = archive; this.community = community; this.identity = identity;
    }

    @GetMapping
    public Result<?> search(@RequestParam(defaultValue = "") String q,
                            @RequestParam(defaultValue = "8") int limit) {
        String kw = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        int max = Math.max(1, Math.min(limit, 20));
        List<Map<String, Object>> groups = new ArrayList<>();
        if (kw.isEmpty()) return Result.ok(Map.of("keyword", q, "total", 0, "groups", groups));

        add(groups, kw, max, "成长记录", timeline.rowsScoped("records", false), r -> "/timeline/records", "title");
        add(groups, kw, max, "里程碑", timeline.rowsScoped("milestones", false), r -> "/timeline/milestones", "title");
        add(groups, kw, max, "工作 / 进阶记录", merge(timeline.rowsScoped("work-records", false),
                timeline.rowsScoped("advance-records", false)), r -> "/timeline/work", "title", "company", "position");
        add(groups, kw, max, "题库", learning.rowsScoped("questions", false), r -> "/learning/questions", "title", "content");
        add(groups, kw, max, "练习场次", learning.rowsScoped("sessions", false), r -> "/learning/sessions", "title");
        add(groups, kw, max, "复习计划 / 目标", merge(planning.rowsScoped("review-plans", false),
                planning.rowsScoped("target-paths", false), planning.rowsScoped("checklist-items", false)),
                r -> "/planning/review", "title", "subject", "content");
        add(groups, kw, max, "志愿方案", planning.rowsScoped("volunteer-plans", false), r -> "/planning/volunteer", "title", "planName");
        add(groups, kw, max, "证书", archive.rowsScoped("certificates"), r -> "/archive/certificates", "name", "issuer");
        add(groups, kw, max, "论文 / 看板", merge(archive.rowsScoped("papers"), archive.rowsScoped("thesis-board")),
                r -> "/archive/papers", "title", "keywords");
        add(groups, kw, max, "课程树 / 选课参考", merge(archive.rowsScoped("course-tree"), archive.rowsScoped("elective-posts")),
                r -> "/archive/course-tree", "name", "courseName");
        add(groups, kw, max, "简历条目", identity.rowsScoped("resume-sections"), r -> "/archive/resume", "title", "sectionType");

        // 广场文章：所有已发布文章 + 本人未公开稿件（后者跳到投稿管理页）
        List<Map<String, Object>> articles = new ArrayList<>(community.published());
        for (Map<String, Object> mine : community.rowsScoped("contents")) {
            if (!"PUBLISHED".equals(String.valueOf(mine.get("status")))) articles.add(mine);
        }
        add(groups, kw, max, "社区文章", articles,
                r -> "PUBLISHED".equals(String.valueOf(r.get("status"))) ? "/community/" + r.get("id") : "/community/publish",
                "title", "tags", "summary");

        int total = 0;
        for (Map<String, Object> g : groups) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) g.get("items");
            total += items.size();
        }
        return Result.ok(Map.of("keyword", q, "total", total, "groups", groups));
    }

    private void add(List<Map<String, Object>> groups, String kw, int limit, String label,
                     List<Map<String, Object>> rows, Function<Map<String, Object>, String> urlFn, String... titleKeys) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String title = first(r, titleKeys);
            if (!title.toLowerCase(Locale.ROOT).contains(kw) && !values(r).contains(kw)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.get("id"));
            m.put("title", title);
            m.put("subtitle", first(r, "summary", "category", "subject", "stage", "status", "issuer",
                    "nodeType", "knowledgePoint", "sectionType", "recordDate"));
            m.put("date", first(r, "recordDate", "occurredAt", "createdAt", "updatedAt", "deadline"));
            m.put("url", urlFn.apply(r));
            items.add(m);
            if (items.size() >= limit) break;
        }
        if (!items.isEmpty()) {
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("label", label);
            g.put("items", items);
            groups.add(g);
        }
    }

    @SafeVarargs
    private static List<Map<String, Object>> merge(List<Map<String, Object>>... parts) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (List<Map<String, Object>> p : parts) out.addAll(p);
        return out;
    }
    private static String first(Map<String, Object> r, String... keys) {
        for (String k : keys) {
            Object v = r.get(k);
            if (v != null && !String.valueOf(v).isBlank()) return String.valueOf(v);
        }
        return "未命名";
    }
    private static String values(Map<String, Object> r) {
        StringBuilder sb = new StringBuilder();
        r.forEach((k, v) -> { if (v != null) sb.append(v).append(' '); });
        return sb.toString().toLowerCase(Locale.ROOT);
    }
}
