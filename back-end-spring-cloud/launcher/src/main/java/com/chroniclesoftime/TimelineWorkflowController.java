package com.chroniclesoftime;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.identity.application.IdentityApplicationService;
import com.chroniclesoftime.shared.Result;
import com.chroniclesoftime.timeline.application.TimelineApplicationService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 时间线跨域工作流：把工作记录 / 进阶记录回写到个人简历分组，
 * 让「时间线上的一条经历」直接成为「档案里的一段经历」，不再重复录入。
 */
@RestController
@RequestMapping("/api/v1/timeline/workflow")
public class TimelineWorkflowController {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TimelineApplicationService timeline;
    private final IdentityApplicationService identity;

    public TimelineWorkflowController(TimelineApplicationService timeline, IdentityApplicationService identity) {
        this.timeline = timeline;
        this.identity = identity;
    }

    /** 已回写数量，供工作 / 进阶页展示同步状态。 */
    @GetMapping("/resume-status")
    public Result<?> resumeStatus() {
        Long uid = AuthContext.userId();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("work", countSynced("work", uid));
        out.put("advance", countSynced("advance", uid));
        return Result.ok(out);
    }

    /**
     * body: { kind: "work" | "advance", ids: [1,2] }
     * ids 为空时同步当前账号该类型的全部记录；已存在的同名分组跳过，不覆盖用户手工编辑。
     */
    @PostMapping("/sync-resume")
    public Result<?> syncResume(@RequestBody Map<String, Object> body) {
        String kind = String.valueOf(body.getOrDefault("kind", "work"));
        if (!"work".equals(kind) && !"advance".equals(kind)) {
            return Result.fail(400, "kind 只能是 work 或 advance");
        }
        String resource = "work".equals(kind) ? "work-records" : "advance-records";
        Long uid = AuthContext.userId();
        Set<Long> picked = new HashSet<>();
        Object rawIds = body.get("ids");
        if (rawIds instanceof Collection<?> c) {
            for (Object o : c) {
                if (o instanceof Number n) picked.add(n.longValue());
                else if (o != null && !String.valueOf(o).isBlank()) {
                    try { picked.add(Long.parseLong(String.valueOf(o))); } catch (NumberFormatException ignored) { /* 忽略非法 id */ }
                }
            }
        }

        Set<String> existing = new HashSet<>();
        for (Map<String, Object> s : identity.rows("resume-sections")) {
            if (uid != null && !uid.equals(longOf(s.get("userId")))) continue;
            existing.add(kind + "|" + String.valueOf(s.getOrDefault("title", "")));
        }

        int created = 0, skipped = 0;
        List<Map<String, Object>> rows = timeline.rowsScoped(resource, false);
        for (Map<String, Object> r : rows) {
            Long id = longOf(r.get("id"));
            if (!picked.isEmpty() && (id == null || !picked.contains(id))) continue;
            String title = "work".equals(kind)
                    ? text(r.get("company")) + " · " + text(r.get("position"))
                    : text(r.get("title"));
            if (title.isBlank() || " · ".equals(title.trim())) title = "未命名经历 #" + id;
            if (existing.contains(kind + "|" + title)) { skipped++; continue; }

            Map<String, Object> section = new LinkedHashMap<>();
            section.put("sectionType", kind);
            section.put("title", title);
            section.put("subtitle", "work".equals(kind) ? text(r.get("category")) : text(r.get("category")));
            section.put("content", "work".equals(kind) ? text(r.get("content")) : text(r.get("description")));
            section.put("startDate", "work".equals(kind) ? text(r.get("startDate")) : text(r.get("occurredAt")));
            section.put("endDate", "work".equals(kind) ? text(r.get("endDate")) : "");
            section.put("sortNo", created + existing.size());
            section.put("createdAt", LocalDateTime.now().format(TS));
            identity.save("resume-sections", section);
            existing.add(kind + "|" + title);
            created++;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("kind", kind);
        out.put("created", created);
        out.put("skipped", skipped);
        out.put("total", rows.size());
        return Result.ok(out);
    }

    private int countSynced(String kind, Long uid) {
        int n = 0;
        for (Map<String, Object> s : identity.rows("resume-sections")) {
            if (uid != null && !uid.equals(longOf(s.get("userId")))) continue;
            if (kind.equals(String.valueOf(s.get("sectionType")))) n++;
        }
        return n;
    }

    private static String text(Object v) { return v == null ? "" : String.valueOf(v); }
    private static Long longOf(Object o) { return o instanceof Number n ? n.longValue() : null; }
}
