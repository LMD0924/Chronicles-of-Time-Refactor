package com.chroniclesoftime.archive.interfaces;

import com.chroniclesoftime.archive.application.ArchiveApplicationService;
import com.chroniclesoftime.identity.application.IdentityApplicationService;
import com.chroniclesoftime.shared.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** 档案域特性：简历聚合、论文建议、课程树与统计。全部按当前账号隔离。 */
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
        List<Map<String, Object>> rows = identity.rowsScoped("resume-sections");
        rows.sort(Comparator.comparingInt(r -> intOf(r.get("sortNo"))));
        for (Map<String, Object> s : rows) {
            grouped.computeIfAbsent(String.valueOf(s.getOrDefault("sectionType", "其他")), k -> new ArrayList<>()).add(s);
        }
        return Result.ok(grouped);
    }

    @GetMapping("/paper/suggestions")
    public Result<?> suggestions(@RequestParam(required = false) Long paperId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> s : service.rowsScoped("paper-suggestions")) {
            if (paperId == null || paperId.equals(longOf(s.get("paperId")))) out.add(s);
        }
        return Result.ok(out);
    }

    @GetMapping("/course-tree")
    public Result<?> courseTree() {
        List<Map<String, Object>> flat = service.rowsScoped("course-tree");
        flat.sort(Comparator.comparingInt(r -> intOf(r.get("sortNo"))));
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
        List<Map<String, Object>> certificates = service.rowsScoped("certificates");
        List<Map<String, Object>> papers = service.rowsScoped("papers");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("certificates", certificates.size());
        m.put("papers", papers.size());
        m.put("courseNodes", service.rowsScoped("course-tree").size());
        m.put("thesisItems", service.rowsScoped("thesis-board").size());
        m.put("electivePosts", service.rowsScoped("elective-posts").size());
        m.put("attachments", certificates.stream().filter(r -> hasText(r.get("fileUrl"))).count()
                + papers.stream().filter(r -> hasText(r.get("fileUrl"))).count());
        m.put("certificateCategories", distinctCount(certificates, "category"));
        return Result.ok(m);
    }

    private static long distinctCount(List<Map<String, Object>> rows, String key) {
        return rows.stream().map(r -> String.valueOf(r.getOrDefault(key, "未分类"))).distinct().count();
    }
    private static boolean hasText(Object v) { return v != null && !String.valueOf(v).isBlank(); }
    private static int intOf(Object o) { return o instanceof Number n ? n.intValue() : 0; }
    private static Long longOf(Object o) { return o instanceof Number n ? n.longValue() : null; }
}
