package com.chroniclesoftime.planning.interfaces;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.planning.application.PlanningApplicationService;
import com.chroniclesoftime.shared.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** 规划域特性：选科推荐与统计、志愿推荐与模拟、GPA/学分/课表、对比。 */
@RestController
@RequestMapping("/api/v1/planning")
public class PlanningFeatureController {
    private final PlanningApplicationService service;
    private final HttpServletRequest request;

    public PlanningFeatureController(PlanningApplicationService service, HttpServletRequest request) {
        this.service = service;
        this.request = request;
    }

    /** 默认按当前账号隔离；仅管理员带 all=true 时可跨账号核查，避免普通用户看到他人数据。 */
    private boolean all() {
        return AuthContext.isAdmin() && "true".equalsIgnoreCase(request.getParameter("all"));
    }
    private List<Map<String, Object>> rows(String resource) { return service.rowsScoped(resource, all()); }

    @GetMapping("/subjects")
    public Result<?> subjects() { return Result.ok(rows("subject-options")); }

    @GetMapping("/combinations/hot")
    public Result<?> hotCombinations() {
        List<Map<String, Object>> list = new ArrayList<>(rows("subject-combinations"));
        list.sort((a, b) -> Double.compare(num(b.get("studentCount")), num(a.get("studentCount"))));
        return Result.ok(list);
    }

    @PostMapping("/selection/recommend")
    public Result<?> recommendSelection(@RequestBody Map<String, Object> body) {
        String first = String.valueOf(body.getOrDefault("firstSubject", ""));
        List<Map<String, Object>> combos = rows("subject-combinations").stream()
                .filter(c -> first.isBlank() || first.equals(String.valueOf(c.getOrDefault("firstSubject", "")))).toList();
        List<Map<String, Object>> majors = rows("major-requirements").stream()
                .filter(m -> first.isBlank() || String.valueOf(m.getOrDefault("requiredSubjects", "")).contains(first)).toList();
        return Result.ok(Map.of("combinations", combos, "majors", majors, "basedOn", first));
    }

    @PostMapping("/selection/confirm")
    public Result<?> confirmSelection(@RequestBody Map<String, Object> body) {
        Object selection = service.save("selections", body);
        Map<String, Object> history = new LinkedHashMap<>();
        history.put("selectionId", body.get("id")); history.put("action", "CONFIRM");
        history.put("operator", body.getOrDefault("operator", "self")); history.put("detail", "确认选科");
        history.put("createdAt", java.time.LocalDateTime.now().toString().substring(0, 19).replace('T', ' '));
        service.save("selection-histories", history);
        return Result.ok(selection);
    }

    @GetMapping("/selection/statistics")
    public Result<?> selectionStats() {
        Map<String, Integer> byStatus = new TreeMap<>();
        List<Map<String, Object>> list = rows("selections");
        for (Map<String, Object> s : list) {
            byStatus.merge(String.valueOf(s.getOrDefault("status", "DRAFT")), 1, Integer::sum);
        }
        return Result.ok(Map.of("byStatus", byStatus, "total", list.size()));
    }

    @PostMapping("/volunteer/recommend")
    public Result<?> recommendVolunteer(@RequestBody Map<String, Object> body) {
        String major = String.valueOf(body.getOrDefault("major", ""));
        double score = num(body.get("score"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> m : rows("major-requirements")) {
            if (!major.isBlank() && !String.valueOf(m.getOrDefault("majorName", "")).contains(major)) continue;
            Map<String, Object> r = new LinkedHashMap<>(m);
            double gap = score - 500;
            r.put("chanceLevel", gap > 80 ? "保" : gap > 20 ? "稳" : "冲");
            r.put("suggestedScore", 500 + (gap > 80 ? -30 : gap > 20 ? 0 : 40));
            out.add(r);
        }
        return Result.ok(out);
    }

    @PostMapping("/volunteer/simulate")
    public Result<?> simulate(@RequestBody Map<String, Object> body) {
        Long planId = Long.valueOf(String.valueOf(body.getOrDefault("planId", 0)));
        List<Map<String, Object>> details = rows("volunteer-details").stream()
                .filter(d -> planId.equals(longOf(d.get("planId")))).toList();
        List<Object> out = new ArrayList<>();
        for (Map<String, Object> d : details) {
            String level = String.valueOf(d.getOrDefault("chanceLevel", "稳"));
            double p = "冲".equals(level) ? 0.38 : "保".equals(level) ? 0.88 : 0.66;
            Map<String, Object> sim = new LinkedHashMap<>();
            sim.put("planId", planId); sim.put("detailId", d.get("id"));
            sim.put("result", p > .6 ? "较有把握" : "存在风险"); sim.put("probability", p);
            sim.put("analysis", level + "档 · 依据历史位次与计划数估算（演示口径）");
            out.add(service.save("simulations", sim));
        }
        return Result.ok(out);
    }

    @GetMapping("/volunteer/report/{planId}")
    public Result<?> volunteerReport(@PathVariable Long planId) {
        Map<String, Integer> dist = new TreeMap<>();
        for (Map<String, Object> d : rows("volunteer-details")) {
            if (planId.equals(longOf(d.get("planId")))) {
                dist.merge(String.valueOf(d.getOrDefault("chanceLevel", "稳")), 1, Integer::sum);
            }
        }
        return Result.ok(Map.of("planId", planId, "distribution", dist));
    }

    @PostMapping("/gpa/simulate")
    public Result<?> simulateGpa(@RequestBody Map<String, Object> body) {
        double target = num(body.get("targetGpa"));
        List<Map<String, Object>> courses = rows("term-courses");
        double credits = courses.stream().mapToDouble(c -> num(c.get("credit"))).sum();
        double points = courses.stream().mapToDouble(c -> num(c.get("gpaPoint")) * num(c.get("credit"))).sum();
        double current = credits == 0 ? 0 : points / credits;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("currentGpa", Math.round(current * 100) / 100.0);
        out.put("targetGpa", target);
        out.put("gap", Math.round((target - current) * 100) / 100.0);
        out.put("totalCredits", credits);
        out.put("courses", courses);
        return Result.ok(out);
    }

    @GetMapping("/gap-analysis")
    public Result<?> gapAnalysis() {
        List<Map<String, Object>> out = rows("graduation-gaps");
        for (Map<String, Object> g : out) {
            double need = num(g.get("requiredCredit")) - num(g.get("earnedCredit"));
            g.put("remaining", Math.max(0, need));
        }
        return Result.ok(out);
    }

    @GetMapping("/schedule")
    public Result<?> schedule() { return Result.ok(rows("term-courses")); }

    @GetMapping("/compare")
    public Result<?> compare(@RequestParam(required = false) Long a, @RequestParam(required = false) Long b) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("plans", rows("volunteer-plans"));
        out.put("details", rows("volunteer-details"));
        out.put("selected", Arrays.asList(a, b));
        return Result.ok(out);
    }

    private static double num(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (RuntimeException e) { return 0; }
    }
    private static Long longOf(Object o) { return o instanceof Number n ? n.longValue() : null; }
}
