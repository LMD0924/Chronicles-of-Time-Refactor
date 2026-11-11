package com.chroniclesoftime.timeline.interfaces;

import com.chroniclesoftime.shared.Result;
import com.chroniclesoftime.timeline.application.TimelineApplicationService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** 时间线域特性：统计、趋势、里程碑、贯通关联。 */
@RestController
@RequestMapping("/api/v1/timeline")
public class TimelineFeatureController {
    private final TimelineApplicationService service;
    public TimelineFeatureController(TimelineApplicationService service) { this.service = service; }

    @GetMapping("/stats")
    public Result<?> stats() {
        List<Map<String, Object>> records = service.rows("records");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalRecords", records.size());
        m.put("milestones", service.rows("milestones").size());
        m.put("workRecords", service.rows("work-records").size());
        m.put("advances", service.rows("advance-records").size());
        m.put("links", service.rows("links").size());
        m.put("studyHours", records.stream().mapToDouble(r -> num(r.get("studyHours"))).sum());
        m.put("avgHappiness", records.isEmpty() ? 0 : records.stream().mapToDouble(r -> num(r.get("happinessIndex"))).average().orElse(0));
        return Result.ok(m);
    }

    @GetMapping("/trend")
    public Result<?> trend(@RequestParam(defaultValue = "14") int days) {
        List<Map<String, Object>> records = service.rows("records");
        Map<String, double[]> byDate = new TreeMap<>();
        for (Map<String, Object> r : records) {
            String d = String.valueOf(r.getOrDefault("recordDate", ""));
            double[] acc = byDate.computeIfAbsent(d, k -> new double[]{0, 0, 0});
            acc[0] += num(r.get("studyHours")); acc[1] += num(r.get("happinessIndex")); acc[2] += 1;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        byDate.forEach((d, v) -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("date", d); p.put("studyHours", v[0]);
            p.put("happiness", v[2] == 0 ? 0 : v[1] / v[2]); p.put("count", (int) v[2]);
            out.add(p);
        });
        return Result.ok(out.size() > days ? out.subList(out.size() - days, out.size()) : out);
    }

    @GetMapping("/milestones")
    public Result<?> milestones() {
        List<Map<String, Object>> ms = new ArrayList<>(service.rows("milestones"));
        for (Map<String, Object> r : service.rows("records")) {
            if (Boolean.TRUE.equals(r.get("isMilestone"))) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("title", r.get("title")); m.put("occurredAt", r.get("recordDate"));
                m.put("description", r.get("content")); m.put("color", "mint");
                ms.add(m);
            }
        }
        return Result.ok(ms);
    }

    @GetMapping("/connections")
    public Result<?> connections() { return Result.ok(service.rows("links")); }

    @PostMapping("/connections")
    public Result<?> connect(@RequestBody Map<String, Object> body) {
        return Result.ok(service.save("links", body));
    }

    private static double num(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (RuntimeException e) { return 0; }
    }
}
