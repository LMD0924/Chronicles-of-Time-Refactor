package com.chroniclesoftime.timeline.interfaces;

import com.chroniclesoftime.shared.Result;
import com.chroniclesoftime.timeline.application.TimelineApplicationService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 时间线域特性：统计、趋势、里程碑双通道联动、贯通关联、成长记录热力与筛选项。
 * 所有读取默认按当前账号隔离；管理员显式 all=true 时可跨账号核查。
 */
@RestController
@RequestMapping("/api/v1/timeline")
public class TimelineFeatureController {
    private final TimelineApplicationService service;
    public TimelineFeatureController(TimelineApplicationService service) { this.service = service; }

    @GetMapping("/stats")
    public Result<?> stats(@RequestParam(defaultValue = "false") boolean all) {
        List<Map<String, Object>> records = service.rowsScoped("records", all);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalRecords", records.size());
        m.put("milestones", service.rowsScoped("milestones", all).size());
        m.put("workRecords", service.rowsScoped("work-records", all).size());
        m.put("advances", service.rowsScoped("advance-records", all).size());
        m.put("links", service.rowsScoped("links", all).size());
        m.put("studyHours", records.stream().mapToDouble(r -> num(r.get("studyHours"))).sum());
        m.put("avgHappiness", records.isEmpty() ? 0 : records.stream().mapToDouble(r -> num(r.get("happinessIndex"))).average().orElse(0));
        return Result.ok(m);
    }

    @GetMapping("/trend")
    public Result<?> trend(@RequestParam(defaultValue = "14") int days,
                           @RequestParam(defaultValue = "false") boolean all) {
        Map<String, double[]> byDate = new TreeMap<>();
        for (Map<String, Object> r : service.rowsScoped("records", all)) {
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

    /** 兼容旧调用：真实里程碑列表（含 origin / 关联记录信息）。 */
    @GetMapping("/milestones")
    public Result<?> milestones(@RequestParam(defaultValue = "false") boolean all) {
        return Result.ok(service.milestoneDetails(all));
    }

    @GetMapping("/milestones/detail")
    public Result<?> milestoneDetails(@RequestParam(defaultValue = "false") boolean all) {
        return Result.ok(service.milestoneDetails(all));
    }

    /** D1 手动通道：把里程碑关联到一条成长记录。 */
    @PostMapping("/milestones/{id}/link")
    public Result<?> linkMilestone(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Object v = body.get("recordId");
            Long recordId = v instanceof Number n ? n.longValue()
                    : (v == null || String.valueOf(v).isBlank()) ? null : Long.parseLong(String.valueOf(v));
            return Result.ok(service.linkMilestone(id, recordId));
        } catch (SecurityException e) {
            return Result.fail(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @DeleteMapping("/milestones/{id}/link")
    public Result<?> unlinkMilestone(@PathVariable Long id) {
        try {
            return Result.ok(service.unlinkMilestone(id));
        } catch (SecurityException e) {
            return Result.fail(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @GetMapping("/connections")
    public Result<?> connections(@RequestParam(defaultValue = "false") boolean all) {
        return Result.ok(service.rowsScoped("links", all));
    }

    @PostMapping("/connections")
    public Result<?> connect(@RequestBody Map<String, Object> body) {
        return Result.ok(service.save("links", body));
    }

    @PutMapping("/connections/{id}")
    public Result<?> updateConnection(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return Result.ok(service.save("links", body));
    }

    @DeleteMapping("/connections/{id}")
    public Result<?> deleteConnection(@PathVariable Long id) {
        return Result.ok(service.delete("links", id));
    }

    /** 成长记录热力：按日期聚合，供日历热力图使用。 */
    @GetMapping("/records/heatmap")
    public Result<?> heatmap(@RequestParam(defaultValue = "90") int days,
                             @RequestParam(defaultValue = "false") boolean all) {
        Map<String, Map<String, Object>> byDate = new TreeMap<>();
        for (Map<String, Object> r : service.rowsScoped("records", all)) {
            String d = String.valueOf(r.getOrDefault("recordDate", ""));
            if (d.isBlank()) continue;
            Map<String, Object> cell = byDate.computeIfAbsent(d, k -> {
                Map<String, Object> c = new LinkedHashMap<>();
                c.put("date", k); c.put("count", 0); c.put("studyHours", 0.0); c.put("happinessSum", 0.0);
                return c;
            });
            cell.put("count", (int) cell.get("count") + 1);
            cell.put("studyHours", num(cell.get("studyHours")) + num(r.get("studyHours")));
            cell.put("happinessSum", num(cell.get("happinessSum")) + num(r.get("happinessIndex")));
        }
        List<Map<String, Object>> out = new ArrayList<>();
        byDate.values().forEach(c -> {
            int count = (int) c.get("count");
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("date", c.get("date")); p.put("count", count);
            p.put("studyHours", c.get("studyHours"));
            p.put("happiness", count == 0 ? 0 : num(c.get("happinessSum")) / count);
            out.add(p);
        });
        return Result.ok(out.size() > days ? out.subList(out.size() - days, out.size()) : out);
    }

    /** 成长记录筛选项：分类 / 时段 / 标签，均由真实数据推导，避免前端写死字典。 */
    @GetMapping("/records/facets")
    public Result<?> facets(@RequestParam(defaultValue = "false") boolean all) {
        Set<String> categories = new TreeSet<>(), periods = new TreeSet<>(), tags = new TreeSet<>();
        for (Map<String, Object> r : service.rowsScoped("records", all)) {
            add(categories, r.get("category"));
            add(periods, r.get("period"));
            String tagLine = r.get("tags") == null ? "" : String.valueOf(r.get("tags"));
            for (String t : tagLine.split("[,，;；\\s]+")) add(tags, t);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("categories", categories); m.put("periods", periods); m.put("tags", tags);
        return Result.ok(m);
    }

    private static void add(Set<String> set, Object value) {
        if (value == null) return;
        String s = String.valueOf(value).trim();
        if (!s.isBlank()) set.add(s);
    }

    private static double num(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (RuntimeException e) { return 0; }
    }
}
