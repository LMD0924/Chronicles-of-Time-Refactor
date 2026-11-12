package com.chroniclesoftime.planning.interfaces;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.planning.application.PlanningApplicationService;
import com.chroniclesoftime.shared.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** 志愿填报工作流：方案 / 明细 / 批量 / 智能推荐入库 / 选科匹配 / 模拟录取 / 统计 / 专业检索。 */
@RestController
@RequestMapping("/api/v1/planning/volunteer")
public class VolunteerWorkflowController {
    private final PlanningApplicationService service;
    private final ObjectMapper json;

    public VolunteerWorkflowController(PlanningApplicationService service, ObjectMapper json) {
        this.service = service;
        this.json = json;
    }

    // ============================================================ 方案

    @GetMapping("/plans")
    public Result<?> plans() { return Result.ok(mine("volunteer-plans")); }

    @PostMapping("/plans")
    public Result<?> addPlan(@RequestBody Map<String, Object> body) {
        String title = str(body.get("title")).trim();
        if (title.isBlank()) return Result.fail(400, "方案名称必填");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("planYear", str(body.get("planYear")));
        m.put("province", str(body.get("province")));
        String status = str(body.get("status"));
        m.put("status", status.isBlank() ? "DRAFT" : status);
        m.put("createdAt", now());
        return Result.ok(toMap(service.save("volunteer-plans", m)));
    }

    @PutMapping("/plans/{id}")
    public Result<?> updatePlan(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> row = anyById("volunteer-plans", id);
        Result<?> deny = ownedOrDeny(row, "方案不存在", "无权操作他人方案");
        if (deny != null) return deny;
        Map<String, Object> up = new LinkedHashMap<>(row);
        for (String k : List.of("title", "planYear", "province", "status")) {
            if (body.containsKey(k)) up.put(k, str(body.get(k)));
        }
        return Result.ok(toMap(service.save("volunteer-plans", up)));
    }

    /** 删除方案时级联删除该方案下志愿明细与模拟结果（仅当前账号数据）。 */
    @DeleteMapping("/plans/{id}")
    public Result<?> deletePlan(@PathVariable Long id) {
        Map<String, Object> row = anyById("volunteer-plans", id);
        Result<?> deny = ownedOrDeny(row, "方案不存在", "无权操作他人方案");
        if (deny != null) return deny;
        for (Map<String, Object> d : mine("volunteer-details")) {
            if (id.equals(asLong(d.get("planId")))) service.delete("volunteer-details", asLong(d.get("id")));
        }
        for (Map<String, Object> s : mine("simulations")) {
            if (id.equals(asLong(s.get("planId")))) service.delete("simulations", asLong(s.get("id")));
        }
        service.delete("volunteer-plans", id);
        return Result.ok(Map.of("deleted", true, "planId", id));
    }

    // ============================================================ 志愿明细

    @GetMapping("/plans/{id}/details")
    public Result<?> planDetails(@PathVariable Long id) {
        Map<String, Object> plan = anyById("volunteer-plans", id);
        Result<?> deny = ownedOrDeny(plan, "方案不存在", "无权访问他人方案");
        if (deny != null) return deny;
        List<Map<String, Object>> out = mine("volunteer-details").stream()
                .filter(d -> id.equals(asLong(d.get("planId"))))
                .sorted((a, b) -> Integer.compare(intOf(a.get("priority")), intOf(b.get("priority"))))
                .collect(Collectors.toCollection(ArrayList::new));
        return Result.ok(out);
    }

    @PostMapping("/details")
    public Result<?> addDetail(@RequestBody Map<String, Object> body) {
        Long planId = asLong(body.get("planId"));
        Result<?> deny = planOwned(planId);
        if (deny != null) return deny;
        return Result.ok(toMap(service.save("volunteer-details", detailBody(body, planId))));
    }

    @PutMapping("/details/{id}")
    public Result<?> updateDetail(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> row = anyById("volunteer-details", id);
        Result<?> deny = ownedOrDeny(row, "志愿不存在", "无权操作他人志愿");
        if (deny != null) return deny;
        Map<String, Object> up = new LinkedHashMap<>(row);
        for (String k : List.of("universityName", "majorName", "chanceLevel", "note")) {
            if (body.containsKey(k)) up.put(k, str(body.get(k)));
        }
        if (body.containsKey("priority")) up.put("priority", intOf(body.get("priority")));
        if (body.containsKey("scoreLine")) up.put("scoreLine", num(body.get("scoreLine")));
        if (body.containsKey("planId")) {
            Long newPlan = asLong(body.get("planId"));
            Result<?> p = planOwned(newPlan);
            if (p != null) return p;
            up.put("planId", newPlan);
        }
        return Result.ok(toMap(service.save("volunteer-details", up)));
    }

    @DeleteMapping("/details/{id}")
    public Result<?> deleteDetail(@PathVariable Long id) {
        Map<String, Object> row = anyById("volunteer-details", id);
        Result<?> deny = ownedOrDeny(row, "志愿不存在", "无权操作他人志愿");
        if (deny != null) return deny;
        for (Map<String, Object> s : mine("simulations")) {
            if (id.equals(asLong(s.get("detailId")))) service.delete("simulations", asLong(s.get("id")));
        }
        service.delete("volunteer-details", id);
        return Result.ok(Map.of("deleted", true, "id", id));
    }

    /** 批量新增，body 为数组；先统一校验方案归属，再逐条写入。 */
    @PostMapping("/details/batch")
    public Result<?> batchDetails(@RequestBody List<Map<String, Object>> body) {
        if (body == null || body.isEmpty()) return Result.fail(400, "批量数据不能为空");
        for (Map<String, Object> b : body) {
            Result<?> deny = planOwned(asLong(b.get("planId")));
            if (deny != null) return deny;
        }
        List<Object> out = new ArrayList<>();
        for (Map<String, Object> b : body) {
            out.add(toMap(service.save("volunteer-details", detailBody(b, asLong(b.get("planId"))))));
        }
        return Result.ok(out);
    }

    /** 智能推荐结果真实写入志愿：校验 planId 归属后保存 volunteer_detail。 */
    @PostMapping("/details/from-recommend")
    public Result<?> fromRecommend(@RequestBody Map<String, Object> body) {
        Long planId = asLong(body.get("planId"));
        Result<?> deny = planOwned(planId);
        if (deny != null) return deny;
        String major = str(body.get("majorName")).trim();
        if (major.isBlank()) return Result.fail(400, "专业名称必填");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("planId", planId);
        m.put("universityName", str(body.get("universityName")));
        m.put("majorName", major);
        m.put("priority", body.get("priority") == null ? 0 : intOf(body.get("priority")));
        m.put("chanceLevel", str(body.get("chanceLevel")));
        m.put("scoreLine", body.get("scoreLine") == null ? 0.0 : num(body.get("scoreLine")));
        m.put("note", body.get("note") == null ? "来自智能推荐" : str(body.get("note")));
        return Result.ok(toMap(service.save("volunteer-details", m)));
    }

    // ============================================================ 选科匹配

    /** 真实选科匹配：按 majorName 查 required_subjects，与当前用户已确认选科组合比对，结果写入 admission_simulation。 */
    @PostMapping("/matching/check/{detailId}")
    public Result<?> matchingCheck(@PathVariable Long detailId) {
        Map<String, Object> detail = anyById("volunteer-details", detailId);
        Result<?> deny = ownedOrDeny(detail, "志愿不存在", "无权操作他人志愿");
        if (deny != null) return deny;
        List<String> chosen = confirmedSubjects();
        Map<String, Object> match = computeMatch(detail, chosen);
        String analysis = "专业选科要求 [" + String.join(",", castList(match.get("requiredSubjects")))
                + "]；已确认选科 [" + String.join(",", chosen) + "]；命中 "
                + match.get("hitCount") + "/" + match.get("requiredCount")
                + "；匹配分 " + match.get("matchingScore");
        Map<String, Object> sim = new LinkedHashMap<>();
        sim.put("planId", detail.get("planId"));
        sim.put("detailId", detailId);
        sim.put("result", Boolean.TRUE.equals(match.get("matched")) ? "MATCHED" : "PARTIAL");
        sim.put("probability", match.get("matchingScore"));
        sim.put("analysis", analysis);
        sim.put("createdAt", now());
        Map<String, Object> out = toMap(service.save("simulations", sim));
        out.putAll(match);
        return Result.ok(out);
    }

    /** 该方案所有志愿的匹配汇总：总数 / 命中数 / 未匹配 / 平均匹配分 / 逐条明细。 */
    @GetMapping("/matching/report/{planId}")
    public Result<?> matchingReport(@PathVariable Long planId) {
        Map<String, Object> plan = anyById("volunteer-plans", planId);
        Result<?> deny = ownedOrDeny(plan, "方案不存在", "无权访问他人方案");
        if (deny != null) return deny;
        List<String> chosen = confirmedSubjects();
        List<Map<String, Object>> details = mine("volunteer-details").stream()
                .filter(d -> planId.equals(asLong(d.get("planId"))))
                .sorted((a, b) -> Integer.compare(intOf(a.get("priority")), intOf(b.get("priority"))))
                .collect(Collectors.toCollection(ArrayList::new));
        List<Map<String, Object>> items = new ArrayList<>();
        int matched = 0, unmatched = 0;
        double sum = 0;
        for (Map<String, Object> d : details) {
            Map<String, Object> m = computeMatch(d, chosen);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("detailId", d.get("id"));
            item.put("universityName", d.get("universityName"));
            item.put("majorName", d.get("majorName"));
            item.putAll(m);
            items.add(item);
            if (Boolean.TRUE.equals(m.get("matched"))) matched++; else unmatched++;
            sum += num(m.get("matchingScore"));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("planId", planId);
        out.put("total", items.size());
        out.put("matched", matched);
        out.put("unmatched", unmatched);
        out.put("averageMatchingScore", items.isEmpty() ? 0.0 : round(sum / items.size()));
        out.put("chosenSubjects", chosen);
        out.put("details", items);
        return Result.ok(out);
    }

    // ============================================================ 模拟录取

    /** 单条模拟：基于 score_line 与方案参考分之差计算概率(封顶 0~1)，结果落库 admission_simulation。 */
    @PostMapping("/simulate/single/{detailId}")
    public Result<?> simulateSingle(@PathVariable Long detailId,
                                    @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> detail = anyById("volunteer-details", detailId);
        Result<?> deny = ownedOrDeny(detail, "志愿不存在", "无权操作他人志愿");
        if (deny != null) return deny;
        Long planId = asLong(detail.get("planId"));
        double line = num(detail.get("scoreLine"));
        double ref = body != null && body.get("score") != null ? num(body.get("score")) : avgScoreLine(planId);
        double gap = ref - line;
        double probability = round(clamp(0.5 + gap / 200.0, 0.0, 1.0));
        String result = probability >= 0.7 ? "ADMITTED" : probability >= 0.4 ? "WAITING" : "REJECTED";
        String analysis = "参考分 " + round(ref) + "，投档线 " + round(line)
                + "，分差 " + round(gap) + "，录取概率 " + probability;
        Map<String, Object> sim = new LinkedHashMap<>();
        sim.put("planId", planId);
        sim.put("detailId", detailId);
        sim.put("result", result);
        sim.put("probability", probability);
        sim.put("analysis", analysis);
        sim.put("createdAt", now());
        Map<String, Object> out = toMap(service.save("simulations", sim));
        out.put("referenceScore", round(ref));
        out.put("scoreLine", round(line));
        return Result.ok(out);
    }

    // ============================================================ 统计 / 检索

    /** 当前用户的方案数、志愿数、各档位分布、平均参考分。 */
    @GetMapping("/statistics")
    public Result<?> statistics() {
        List<Map<String, Object>> plans = mine("volunteer-plans");
        List<Map<String, Object>> details = mine("volunteer-details");
        Map<String, Integer> levelDist = new TreeMap<>();
        double sum = 0;
        int count = 0;
        for (Map<String, Object> d : details) {
            levelDist.merge(str(d.get("chanceLevel")).isBlank() ? "未设置" : str(d.get("chanceLevel")), 1, Integer::sum);
            if (d.get("scoreLine") != null) { sum += num(d.get("scoreLine")); count++; }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("planCount", plans.size());
        out.put("detailCount", details.size());
        out.put("chanceLevelDistribution", levelDist);
        out.put("averageScoreLine", count == 0 ? 0.0 : round(sum / count));
        return Result.ok(out);
    }

    /** 基于 major_requirement 的关键词检索（代码 / 名称 / 门类 / 选科要求）。 */
    @GetMapping("/search/majors")
    public Result<?> searchMajors(@RequestParam(required = false) String keyword) {
        String kw = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> m : service.rows("major-requirements")) {
            if (kw.isBlank()
                    || str(m.get("majorName")).toLowerCase(Locale.ROOT).contains(kw)
                    || str(m.get("majorCode")).toLowerCase(Locale.ROOT).contains(kw)
                    || str(m.get("category")).toLowerCase(Locale.ROOT).contains(kw)
                    || str(m.get("requiredSubjects")).toLowerCase(Locale.ROOT).contains(kw)) {
                out.add(m);
            }
        }
        return Result.ok(out);
    }

    // ============================================================ helpers

    private Long uid() { return AuthContext.userId(); }

    private List<Map<String, Object>> mine(String resource) {
        Long id = uid();
        if (id == null) return new ArrayList<>();
        return service.rows(resource).stream()
                .filter(r -> id.equals(asLong(r.get("userId"))))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Map<String, Object> anyById(String resource, Long id) {
        if (id == null) return null;
        return service.rows(resource).stream()
                .filter(r -> id.equals(asLong(r.get("id"))))
                .findFirst().orElse(null);
    }

    private Result<?> ownedOrDeny(Map<String, Object> row, String missing, String forbidden) {
        if (row == null) return Result.fail(404, missing);
        if (!Objects.equals(uid(), asLong(row.get("userId")))) return Result.fail(403, forbidden);
        return null;
    }

    /** 校验 planId 存在且属于当前账号；返回 null 表示通过。 */
    private Result<?> planOwned(Long planId) {
        if (planId == null) return Result.fail(400, "planId 必填");
        Map<String, Object> plan = anyById("volunteer-plans", planId);
        if (plan == null) return Result.fail(404, "方案不存在");
        if (!Objects.equals(uid(), asLong(plan.get("userId")))) return Result.fail(403, "无权向他人方案添加志愿");
        return null;
    }

    private Map<String, Object> detailBody(Map<String, Object> body, Long planId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("planId", planId);
        m.put("universityName", str(body.get("universityName")));
        m.put("majorName", str(body.get("majorName")));
        m.put("priority", body.get("priority") == null ? 0 : intOf(body.get("priority")));
        m.put("chanceLevel", str(body.get("chanceLevel")));
        m.put("scoreLine", body.get("scoreLine") == null ? 0.0 : num(body.get("scoreLine")));
        m.put("note", str(body.get("note")));
        return m;
    }

    /** 当前用户已确认(CONFIRMED / APPROVED)选科的组合科目。 */
    private List<String> confirmedSubjects() {
        List<Map<String, Object>> sels = mine("selections").stream()
                .filter(s -> {
                    String st = str(s.get("status")).toUpperCase(Locale.ROOT);
                    return "CONFIRMED".equals(st) || "APPROVED".equals(st);
                })
                .sorted((a, b) -> Long.compare(lid(b), lid(a)))
                .collect(Collectors.toCollection(ArrayList::new));
        if (sels.isEmpty()) return new ArrayList<>();
        Map<String, Object> combo = anyById("subject-combinations", asLong(sels.get(0).get("combinationId")));
        List<String> out = new ArrayList<>();
        if (combo != null) {
            String f = str(combo.get("firstSubject"));
            if (!f.isBlank()) out.add(f);
            out.addAll(splitSubjects(str(combo.get("secondSubjects"))));
        }
        return out;
    }

    /** 单条志愿的选科匹配：required_subjects 按逗号拆开，计算命中数与匹配分。 */
    private Map<String, Object> computeMatch(Map<String, Object> detail, List<String> chosen) {
        String majorName = str(detail.get("majorName")).trim();
        List<String> req = new ArrayList<>();
        for (Map<String, Object> m : service.rows("major-requirements")) {
            String mn = str(m.get("majorName"));
            if (!mn.isBlank() && (mn.contains(majorName) || majorName.contains(mn))) {
                req.addAll(splitSubjects(str(m.get("requiredSubjects"))));
            }
        }
        req = new ArrayList<>(new LinkedHashSet<>(req));
        int hits = 0;
        for (String r : req) if (chosen.contains(r)) hits++;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("requiredSubjects", req);
        out.put("chosenSubjects", chosen);
        out.put("hitCount", hits);
        out.put("requiredCount", req.size());
        out.put("matchingScore", req.isEmpty() ? 0.0 : round((double) hits / req.size()));
        out.put("matched", !req.isEmpty() && hits == req.size());
        return out;
    }

    private double avgScoreLine(Long planId) {
        List<Map<String, Object>> ds = mine("volunteer-details").stream()
                .filter(d -> planId.equals(asLong(d.get("planId"))))
                .collect(Collectors.toCollection(ArrayList::new));
        if (ds.isEmpty()) return 0;
        return ds.stream().mapToDouble(d -> num(d.get("scoreLine"))).average().orElse(0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object o) { return json.convertValue(o, Map.class); }

    @SuppressWarnings("unchecked")
    private static List<String> castList(Object o) { return o instanceof List ? (List<String>) o : List.of(); }

    private static List<String> splitSubjects(String s) {
        if (s == null || s.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        for (String p : s.split("[,，、/;；\\s]+")) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static double num(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        if (o == null) return 0;
        try { return Double.parseDouble(String.valueOf(o)); } catch (NumberFormatException e) { return 0; }
    }

    private static int intOf(Object o) { return (int) num(o); }

    private static Long asLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o == null) return null;
        try { return Long.valueOf(String.valueOf(o).trim()); } catch (NumberFormatException e) { return null; }
    }

    private static long lid(Map<String, Object> m) { Long v = asLong(m.get("id")); return v == null ? 0L : v; }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    private static String now() { return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
}
