package com.chroniclesoftime.planning.interfaces;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.planning.application.PlanningApplicationService;
import com.chroniclesoftime.shared.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 规划域业务工作流：把「复习计划 / 目标路径 / 综评 / 强基清单 / GPA / 毕业缺口 / 课表 /
 * 等级量表 / 专业要求 / 组合树」从一张张独立表格串成可调参、可推进、可统计的闭环。
 */
@RestController
@RequestMapping("/api/v1/planning/workflow")
public class PlanningWorkflowController {
    private static final List<String> REVIEW_STAGES = List.of("启动", "基础复习", "强化训练", "冲刺复盘");

    private final PlanningApplicationService service;
    private final HttpServletRequest request;

    public PlanningWorkflowController(PlanningApplicationService service, HttpServletRequest request) {
        this.service = service;
        this.request = request;
    }

    private boolean all() {
        return AuthContext.isAdmin() && "true".equalsIgnoreCase(request.getParameter("all"));
    }
    private List<Map<String, Object>> rows(String resource) { return service.rowsScoped(resource, all()); }

    /** 规划总览：一次拿齐所有规划页要用的汇总数字。 */
    @GetMapping("/overview")
    public Result<?> overview() {
        List<Map<String, Object>> review = rows("review-plans");
        List<Map<String, Object>> paths = rows("target-paths");
        List<Map<String, Object>> checks = rows("checklist-items");
        List<Map<String, Object>> courses = rows("term-courses");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("reviewPlans", review.size());
        out.put("reviewDone", review.stream().filter(r -> num(r.get("progress")) >= 100
                || "已完成".equals(text(r.get("status")))).count());
        out.put("reviewProgress", avg(review.stream().map(r -> num(r.get("progress"))).toList()));
        out.put("reviewOverdue", review.stream().filter(r -> num(r.get("progress")) < 100 && overdue(r.get("targetDate"))).count());
        out.put("targetStages", paths.size());
        out.put("targetDone", paths.stream().filter(p -> "已完成".equals(text(p.get("status")))).count());
        out.put("checklistTotal", checks.size());
        out.put("checklistDone", checks.stream().filter(c -> Boolean.TRUE.equals(c.get("done"))).count());
        out.put("qualityRecords", rows("quality-records").size());
        out.put("totalCredits", round(courses.stream().mapToDouble(c -> num(c.get("credit"))).sum()));
        out.put("volunteerPlans", rows("volunteer-plans").size());
        out.put("majorRequirements", rows("major-requirements").size());
        return Result.ok(out);
    }

    /** 复习计划看板：状态/学科分组、平均进度、逾期与剩余天数。 */
    @GetMapping("/review/board")
    public Result<?> reviewBoard() {
        String today = LocalDate.now().toString();
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Integer> byStatus = new TreeMap<>();
        Map<String, double[]> bySubject = new TreeMap<>(); // [count, progressSum]
        for (Map<String, Object> r : rows("review-plans")) {
            Map<String, Object> item = new LinkedHashMap<>(r);
            double progress = Math.max(0, Math.min(100, num(r.get("progress"))));
            String date = text(r.get("targetDate"));
            item.put("overdue", progress < 100 && !date.isBlank() && date.compareTo(today) < 0);
            item.put("remainingDays", date.isBlank() || date.length() < 10 ? null : daysBetween(today, date));
            item.put("progress", progress);
            items.add(item);
            byStatus.merge(text(r.get("status")).isBlank() ? "未开始" : text(r.get("status")), 1, Integer::sum);
            String subject = text(r.get("subject")).isBlank() ? "未分类" : text(r.get("subject"));
            double[] agg = bySubject.computeIfAbsent(subject, k -> new double[2]);
            agg[0]++; agg[1] += progress;
        }
        items.sort((a, b) -> text(a.get("targetDate")).compareTo(text(b.get("targetDate"))));
        List<Map<String, Object>> subjectStats = new ArrayList<>();
        bySubject.forEach((k, v) -> subjectStats.add(Map.of(
                "subject", k, "count", (int) v[0], "averageProgress", round(v[1] / v[0]))));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("byStatus", byStatus);
        out.put("bySubject", subjectStats);
        out.put("averageProgress", avg(items.stream().map(i -> (Double) i.get("progress")).toList()));
        return Result.ok(out);
    }

    /** 拖动/输入进度：100% 自动置「已完成」，回到 0 自动置「未开始」，中间为「进行中」。 */
    @PutMapping("/review/{id}/progress")
    public Result<?> setReviewProgress(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> row = service.row("review-plans", id);
        if (row == null) return Result.fail(404, "复习计划不存在: " + id);
        if (!owned(row)) return Result.fail(403, "无权操作其他账号的数据");
        int progress = (int) Math.max(0, Math.min(100, num(body.get("progress"))));
        Map<String, Object> update = new LinkedHashMap<>(row);
        update.put("progress", progress);
        update.put("status", text(body.get("status")).isBlank()
                ? (progress >= 100 ? "已完成" : progress <= 0 ? "未开始" : "进行中")
                : text(body.get("status")));
        return Result.ok(service.save("review-plans", update));
    }

    /** 把一条复习计划拆成目标路径里程碑；不传 steps 时按四阶段自动分解。 */
    @PostMapping("/review/{id}/milestones")
    public Result<?> decompose(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> plan = service.row("review-plans", id);
        if (plan == null) return Result.fail(404, "复习计划不存在: " + id);
        if (!owned(plan)) return Result.fail(403, "无权操作其他账号的数据");
        String target = text(plan.get("title"));
        String deadline = text(plan.get("targetDate"));
        List<Map<String, Object>> steps = new ArrayList<>();
        Object raw = body == null ? null : body.get("steps");
        if (raw instanceof Collection<?> c) {
            for (Object o : c) {
                if (o instanceof Map<?, ?> m) {
                    Map<String, Object> s = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> e : m.entrySet()) s.put(String.valueOf(e.getKey()), e.getValue());
                    steps.add(s);
                }
            }
        }
        if (steps.isEmpty()) {
            for (String stage : REVIEW_STAGES) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("stage", stage); s.put("milestone", stage + " · " + target);
                steps.add(s);
            }
        }
        int created = 0;
        List<Object> out = new ArrayList<>();
        for (Map<String, Object> s : steps) {
            String milestone = text(s.get("milestone")).isBlank()
                    ? text(s.get("stage")) + " · " + target : text(s.get("milestone"));
            Map<String, Object> path = new LinkedHashMap<>();
            path.put("targetName", target);
            path.put("stage", text(s.get("stage")).isBlank() ? "自定义" : text(s.get("stage")));
            path.put("milestone", milestone);
            path.put("deadline", text(s.get("deadline")).isBlank() ? deadline : text(s.get("deadline")));
            path.put("status", "未开始");
            path.put("note", "由复习计划 #" + id + " 分解生成");
            out.add(service.save("target-paths", path));
            created++;
        }
        return Result.ok(Map.of("planId", id, "created", created, "items", out));
    }

    @PutMapping("/target/{id}/status")
    public Result<?> setTargetStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> row = service.row("target-paths", id);
        if (row == null) return Result.fail(404, "目标阶段不存在: " + id);
        if (!owned(row)) return Result.fail(403, "无权操作其他账号的数据");
        Map<String, Object> update = new LinkedHashMap<>(row);
        update.put("status", text(body.get("status")).isBlank() ? "进行中" : text(body.get("status")));
        return Result.ok(service.save("target-paths", update));
    }

    @PutMapping("/checklist/{id}/toggle")
    public Result<?> toggleChecklist(@PathVariable Long id) {
        Map<String, Object> row = service.row("checklist-items", id);
        if (row == null) return Result.fail(404, "清单项不存在: " + id);
        if (!owned(row)) return Result.fail(403, "无权操作其他账号的数据");
        Map<String, Object> update = new LinkedHashMap<>(row);
        update.put("done", !Boolean.TRUE.equals(row.get("done")));
        Object saved = service.save("checklist-items", update);
        List<Map<String, Object>> allItems = rows("checklist-items");
        long done = allItems.stream().filter(c -> Boolean.TRUE.equals(c.get("done"))).count();
        return Result.ok(Map.of("item", saved, "done", done, "total", allItems.size(),
                "completionRate", allItems.isEmpty() ? 0 : round(done * 100.0 / allItems.size())));
    }

    /** 综评分类统计 + 时间线，供图表与卡片复用。 */
    @GetMapping("/quality/summary")
    public Result<?> qualitySummary() {
        Map<String, double[]> byCategory = new TreeMap<>();
        List<Map<String, Object>> items = new ArrayList<>(rows("quality-records"));
        for (Map<String, Object> q : items) {
            String cat = text(q.get("category")).isBlank() ? "未分类" : text(q.get("category"));
            double[] agg = byCategory.computeIfAbsent(cat, k -> new double[2]);
            agg[0]++; agg[1] += num(q.get("score"));
        }
        List<Map<String, Object>> categories = new ArrayList<>();
        byCategory.forEach((k, v) -> categories.add(Map.of(
                "category", k, "count", (int) v[0], "totalScore", round(v[1]), "average", round(v[1] / v[0]))));
        items.sort((a, b) -> text(b.get("occurredAt")).compareTo(text(a.get("occurredAt"))));
        return Result.ok(Map.of("categories", categories, "items", items,
                "totalScore", round(items.stream().mapToDouble(q -> num(q.get("score"))).sum())));
    }

    /** 分数 → 等级换算：按启用的量表区间匹配。 */
    @PostMapping("/grading/convert")
    public Result<?> convertGrade(@RequestBody Map<String, Object> body) {
        double score = num(body.get("score"));
        Long subjectId = body.get("subjectId") instanceof Number n ? n.longValue() : null;
        List<Map<String, Object>> scales = rows("grading-scales").stream()
                .filter(s -> !Boolean.FALSE.equals(s.get("active")))
                .filter(s -> subjectId == null || (s.get("subjectId") instanceof Number n && n.longValue() == subjectId))
                .sorted((a, b) -> Double.compare(num(b.get("minScore")), num(a.get("minScore"))))
                .toList();
        Map<String, Object> hit = null;
        for (Map<String, Object> s : scales) {
            if (score >= num(s.get("minScore")) && score <= num(s.get("maxScore"))) { hit = s; break; }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("score", score);
        out.put("matched", hit != null);
        out.put("level", hit == null ? "未匹配" : text(hit.get("level")));
        out.put("ratio", hit == null ? 0 : num(hit.get("ratio")));
        out.put("scales", scales);
        return Result.ok(out);
    }

    /** 专业选科匹配：subjects 为逗号分隔的已选科目，返回每个专业缺哪些要求。 */
    @GetMapping("/majors/match")
    public Result<?> matchMajors(@RequestParam(required = false, defaultValue = "") String subjects) {
        Set<String> picked = new LinkedHashSet<>();
        for (String s : subjects.split("[,，、\\s]+")) if (!s.isBlank()) picked.add(s.trim());
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> m : rows("major-requirements")) {
            String required = text(m.get("requiredSubjects"));
            List<String> missing = new ArrayList<>();
            if (!required.isBlank()) {
                for (String one : required.split("[,，、/\\s]+")) {
                    if (!one.isBlank() && !picked.contains(one.trim())) missing.add(one.trim());
                }
            }
            Map<String, Object> item = new LinkedHashMap<>(m);
            item.put("satisfied", missing.isEmpty());
            item.put("missing", missing);
            items.add(item);
        }
        items.sort(Comparator.comparingInt((Map<String, Object> m) -> ((List<?>) m.get("missing")).size())
                .thenComparing(m -> text(m.get("majorName"))));
        return Result.ok(Map.of("subjects", picked, "total", items.size(),
                "matched", items.stream().filter(i -> Boolean.TRUE.equals(i.get("satisfied"))).count(), "items", items));
    }

    /** 组合树：按首选科目分组，供树状图与卡片使用。 */
    @GetMapping("/combinations/tree")
    public Result<?> combinationTree() {
        Map<String, List<Map<String, Object>>> grouped = new TreeMap<>();
        for (Map<String, Object> c : rows("subject-combinations")) {
            grouped.computeIfAbsent(text(c.get("firstSubject")).isBlank() ? "其他" : text(c.get("firstSubject")),
                    k -> new ArrayList<>()).add(c);
        }
        List<Map<String, Object>> tree = new ArrayList<>();
        grouped.forEach((k, v) -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("firstSubject", k);
            node.put("count", v.size());
            node.put("students", (int) v.stream().mapToDouble(c -> num(c.get("studentCount"))).sum());
            node.put("children", v);
            tree.add(node);
        });
        return Result.ok(tree);
    }

    /** GPA 可调参推演：按课程名覆盖绩点，返回调整前后对比，不落库。 */
    @PostMapping("/gpa/whatif")
    public Result<?> gpaWhatIf(@RequestBody Map<String, Object> body) {
        double target = num(body.get("targetGpa"));
        Map<String, Double> overrides = new HashMap<>();
        Object raw = body.get("adjustments");
        if (raw instanceof Collection<?> c) {
            for (Object o : c) {
                if (o instanceof Map<?, ?> m) {
                    String name = text(m.get("courseName"));
                    if (!name.isBlank()) overrides.put(name, num(m.get("gpaPoint")));
                }
            }
        }
        List<Map<String, Object>> courses = rows("term-courses");
        double credits = 0, beforePoints = 0, afterPoints = 0;
        List<Map<String, Object>> detail = new ArrayList<>();
        for (Map<String, Object> c : courses) {
            double credit = num(c.get("credit"));
            double before = num(c.get("gpaPoint"));
            double after = overrides.getOrDefault(text(c.get("courseName")), before);
            credits += credit; beforePoints += before * credit; afterPoints += after * credit;
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("courseName", text(c.get("courseName")));
            d.put("credit", credit); d.put("before", before); d.put("after", after);
            detail.add(d);
        }
        double before = credits == 0 ? 0 : beforePoints / credits;
        double after = credits == 0 ? 0 : afterPoints / credits;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("currentGpa", round(before));
        out.put("simulatedGpa", round(after));
        out.put("delta", round(after - before));
        out.put("targetGpa", target);
        out.put("gapToTarget", round(target - after));
        out.put("totalCredits", credits);
        out.put("adjustedCourses", overrides.size());
        out.put("courses", detail);
        return Result.ok(out);
    }

    /** 毕业缺口核对：把课程学分按类别汇总，与毕业要求逐项比对。 */
    @GetMapping("/gap/check")
    public Result<?> gapCheck() {
        List<Map<String, Object>> courses = rows("term-courses");
        Map<String, Double> byCategory = new TreeMap<>();
        for (Map<String, Object> c : courses) {
            byCategory.merge(text(c.get("category")).isBlank() ? "未分类" : text(c.get("category")),
                    num(c.get("credit")), Double::sum);
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> g : rows("graduation-gaps")) {
            Map<String, Object> item = new LinkedHashMap<>(g);
            double need = num(g.get("requiredCredit")) - num(g.get("earnedCredit"));
            item.put("remaining", round(Math.max(0, need)));
            item.put("satisfied", need <= 0);
            items.add(item);
        }
        return Result.ok(Map.of("items", items,
                "totalRemaining", round(items.stream().mapToDouble(i -> num(i.get("remaining"))).sum()),
                "creditsByCategory", byCategory,
                "totalCourseCredits", round(courses.stream().mapToDouble(c -> num(c.get("credit"))).sum())));
    }

    /** 学期课表网格：按学期聚合并统计学分、均分、平均绩点。 */
    @GetMapping("/schedule/grid")
    public Result<?> scheduleGrid() {
        Map<String, List<Map<String, Object>>> grouped = new TreeMap<>();
        for (Map<String, Object> c : rows("term-courses")) {
            grouped.computeIfAbsent(text(c.get("term")).isBlank() ? "未分学期" : text(c.get("term")),
                    k -> new ArrayList<>()).add(c);
        }
        List<Map<String, Object>> terms = new ArrayList<>();
        grouped.forEach((term, list) -> {
            double credits = list.stream().mapToDouble(c -> num(c.get("credit"))).sum();
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("term", term);
            node.put("courses", list);
            node.put("count", list.size());
            node.put("totalCredit", round(credits));
            node.put("averageScore", round(avg(list.stream().map(c -> num(c.get("score"))).toList())));
            node.put("averageGpa", credits == 0 ? 0 : round(list.stream()
                    .mapToDouble(c -> num(c.get("gpaPoint")) * num(c.get("credit"))).sum() / credits));
            terms.add(node);
        });
        return Result.ok(terms);
    }

    private boolean owned(Map<String, Object> row) {
        Long uid = AuthContext.userId();
        if (uid == null || AuthContext.isAdmin()) return true;
        Object owner = row.get("userId");
        return !(owner instanceof Number n) || uid.equals(n.longValue());
    }
    private static boolean overdue(Object date) {
        String d = text(date);
        return !d.isBlank() && d.length() >= 10 && d.substring(0, 10).compareTo(LocalDate.now().toString()) < 0;
    }
    private static long daysBetween(String from, String to) {
        try { return LocalDate.parse(to.substring(0, 10)).toEpochDay() - LocalDate.parse(from).toEpochDay(); }
        catch (RuntimeException e) { return 0; }
    }
    private static double avg(List<Double> values) {
        return values.isEmpty() ? 0 : round(values.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }
    private static double round(double v) { return Math.round(v * 100) / 100.0; }
    private static double num(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (RuntimeException e) { return 0; }
    }
    private static String text(Object v) { return v == null ? "" : String.valueOf(v); }
}
