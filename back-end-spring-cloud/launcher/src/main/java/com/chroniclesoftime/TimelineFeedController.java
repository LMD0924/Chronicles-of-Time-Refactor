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
                          @RequestParam(defaultValue = "false") boolean all) {
        Long uid = AuthContext.userId();
        boolean crossAccount = all && AuthContext.isAdmin();
        List<Map<String, Object>> entries = new ArrayList<>();
        add(entries, "记录", scoped(timeline.rows("records"), uid, crossAccount), "recordDate", "title", "content", "mint", "/timeline/records");
        add(entries, "里程碑", scoped(timeline.rows("milestones"), uid, crossAccount), "occurredAt", "title", "description", "amber", "/timeline/milestones");
        add(entries, "工作", scoped(timeline.rows("work-records"), uid, crossAccount), "startDate", "position", "content", "teal", "/timeline/work");
        add(entries, "进阶", scoped(timeline.rows("advance-records"), uid, crossAccount), "occurredAt", "title", "description", "violet", "/timeline/advances");
        add(entries, "成绩", scoped(learning.rows("scores"), uid, crossAccount), "examDate", "examName", "score", "sky", "/learning/scores");
        add(entries, "练习", scoped(learning.rows("sessions"), uid, crossAccount), "startedAt", "title", "correctCount", "mint", "/learning/practice");
        add(entries, "规划", scoped(planning.rows("review-plans"), uid, crossAccount), "targetDate", "title", "content", "blue", "/planning/review");
        add(entries, "目标", scoped(planning.rows("target-paths"), uid, crossAccount), "deadline", "targetName", "milestone", "rose", "/planning/target");
        add(entries, "文章", community.rows("contents"), "createdAt", "title", "summary", "violet", "/community");
        add(entries, "证书", scoped(archive.rows("certificates"), uid, crossAccount), "issuedAt", "name", "issuer", "amber", "/archive/certificates");
        add(entries, "论文", scoped(archive.rows("papers"), uid, crossAccount), "createdAt", "title", "abstractText", "teal", "/archive/papers");
        entries.removeIf(e -> e.get("occurredAt") == null || String.valueOf(e.get("occurredAt")).isBlank());
        if (type != null && !type.isBlank()) entries.removeIf(e -> !type.equals(e.get("type")));
        entries.sort((a, b) -> String.valueOf(b.get("occurredAt")).compareTo(String.valueOf(a.get("occurredAt"))));
        return Result.ok(entries);
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
            out.add(e);
        }
    }
}
