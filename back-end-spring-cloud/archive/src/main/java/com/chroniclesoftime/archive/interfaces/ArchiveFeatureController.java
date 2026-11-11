package com.chroniclesoftime.archive.interfaces;

import com.chroniclesoftime.archive.application.ArchiveApplicationService;
import com.chroniclesoftime.identity.application.IdentityApplicationService;
import com.chroniclesoftime.shared.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** 档案域特性：简历聚合、论文建议、课程树、证书与统计。 */
@RestController
@RequestMapping("/api/v1/archive")
public class ArchiveFeatureController {
    private final ArchiveApplicationService service;
    private final IdentityApplicationService identity;
    public ArchiveFeatureController(ArchiveApplicationService service, IdentityApplicationService identity) {
        this.service = service;
        this.identity = identity;
    }

    @GetMapping("/resume/complete")
    public Result<?> completeResume() {
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> s : identity.rows("resume-sections")) {
            grouped.computeIfAbsent(String.valueOf(s.getOrDefault("sectionType", "其他")), k -> new ArrayList<>()).add(s);
        }
        return Result.ok(grouped);
    }

    @GetMapping("/paper/suggestions")
    public Result<?> suggestions(@RequestParam(required = false) Long paperId) {
        List<Map<String, Object>> list = service.rows("paper-suggestions").stream()
                .filter(s -> paperId == null || paperId.equals(longOf(s.get("paperId")))).toList();
        return Result.ok(list);
    }

    @GetMapping("/course-tree")
    public Result<?> courseTree() {
        List<Map<String, Object>> flat = service.rows("course-tree");
        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> n : flat) {
            Map<String, Object> copy = new LinkedHashMap<>(n);
            copy.put("children", new ArrayList<Map<String, Object>>());
            byId.put(longOf(n.get("id")), copy);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> n : flat) {
            Long parent = longOf(n.get("parentId"));
            Map<String, Object> node = byId.get(longOf(n.get("id")));
            if (parent == null || parent == 0 || !byId.containsKey(parent)) roots.add(node);
            else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) byId.get(parent).get("children");
                children.add(node);
            }
        }
        return Result.ok(roots);
    }

    @GetMapping("/stats")
    public Result<?> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("certificates", service.rows("certificates").size());
        m.put("papers", service.rows("papers").size());
        m.put("courseNodes", service.rows("course-tree").size());
        m.put("thesisItems", service.rows("thesis-board").size());
        return Result.ok(m);
    }

    private static Long longOf(Object o) { return o instanceof Number n ? n.longValue() : null; }
}
