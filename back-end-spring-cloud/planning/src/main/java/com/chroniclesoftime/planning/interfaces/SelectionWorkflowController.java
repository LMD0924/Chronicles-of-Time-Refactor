package com.chroniclesoftime.planning.interfaces;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.planning.application.PlanningApplicationService;
import com.chroniclesoftime.shared.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** 选科中心工作流：我的选科、提交/确认/修改/退选、意向与指导、历史、审批、统计、建议、专业匹配。 */
@RestController
@RequestMapping("/api/v1/planning/selection")
public class SelectionWorkflowController {
    private static final Set<String> INTENTION_STATUS = Set.of("DRAFT", "EVALUATED", "CONFIRMED", "ADOPTED");
    private static final Set<String> GUIDANCE_STATUS = Set.of("ONGOING", "COMPLETED", "ADOPTED");

    private final PlanningApplicationService service;
    private final ObjectMapper json;
    private final JdbcTemplate jdbc;

    public SelectionWorkflowController(PlanningApplicationService service, ObjectMapper json, JdbcTemplate jdbc) {
        this.service = service;
        this.json = json;
        this.jdbc = jdbc;
    }

    // ============================================================ 我的选科 / 提交

    /** 我的选科：内存 LEFT JOIN 组合维度，并携带该选科最近一条变更历史。 */
    @GetMapping("/mine")
    public Result<?> mine() {
        Map<Long, Map<String, Object>> combos = comboIndex();
        List<Map<String, Object>> histories = mine("selection-histories");
        Map<Long, Map<String, Object>> latest = new HashMap<>();
        for (Map<String, Object> h : histories) {
            Long sid = asLong(h.get("selectionId"));
            if (sid == null) continue;
            Map<String, Object> cur = latest.get(sid);
            if (cur == null) { latest.put(sid, h); continue; }
            int cmp = str(h.get("createdAt")).compareTo(str(cur.get("createdAt")));
            if (cmp > 0 || (cmp == 0 && lid(h) > lid(cur))) latest.put(sid, h);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> s : mine("selections")) {
            Map<String, Object> row = new LinkedHashMap<>(s);
            Map<String, Object> combo = combos.get(asLong(s.get("combinationId")));
            row.put("firstSubject", combo == null ? null : combo.get("firstSubject"));
            row.put("secondSubjects", combo == null ? null : combo.get("secondSubjects"));
            row.put("label", combo == null ? null : combo.get("label"));
            row.put("latestHistory", latest.get(asLong(s.get("id"))));
            out.add(row);
        }
        return Result.ok(out);
    }

    /** 提交选科：写组合 + 选科(PENDING) + 历史(SUBMIT)，返回带组合信息的新记录。 */
    @PostMapping("/submit")
    public Result<?> submit(@RequestBody Map<String, Object> body) {
        String first = str(body.get("firstSubject")).trim();
        String second = str(body.get("secondSubjects")).trim();
        String note = str(body.get("note"));
        if (first.isBlank()) return Result.fail(400, "首选科目必填");
        if (second.isBlank()) return Result.fail(400, "再选科目必填");

        Map<String, Object> combo = new LinkedHashMap<>();
        combo.put("firstSubject", first);
        combo.put("secondSubjects", second);
        combo.put("label", first + "+" + second);
        combo.put("studentCount", 0);
        combo.put("coverage", 0.0);
        combo.put("createdAt", now());
        Long comboId = asLong(toMap(service.save("subject-combinations", combo)).get("id"));

        Map<String, Object> sel = new LinkedHashMap<>();
        sel.put("combinationId", comboId);
        sel.put("status", "PENDING");
        sel.put("note", note);
        sel.put("createdAt", now());
        Map<String, Object> saved = toMap(service.save("selections", sel));
        Long selectionId = asLong(saved.get("id"));
        writeHistory(selectionId, "SUBMIT", "提交选科：" + first + " + " + second
                + (note.isBlank() ? "" : "；理由：" + note));

        Map<String, Object> out = new LinkedHashMap<>(saved);
        out.put("firstSubject", first);
        out.put("secondSubjects", second);
        out.put("label", combo.get("label"));
        return Result.ok(out);
    }

    /** 确认：状态改 CONFIRMED、写确认时间、留痕。 */
    @PutMapping("/confirm/{id}")
    public Result<?> confirm(@PathVariable Long id) {
        Map<String, Object> row = anyById("selections", id);
        Result<?> deny = ownedOrDeny(row, "选科记录不存在", "无权操作他人选科");
        if (deny != null) return deny;
        Map<String, Object> up = new LinkedHashMap<>(row);
        up.put("status", "CONFIRMED");
        up.put("confirmedAt", now());
        Object saved = service.save("selections", up);
        writeHistory(id, "CONFIRM", "确认选科");
        return Result.ok(toMap(saved));
    }

    /** 修改：更新组合与备注，状态回到 PENDING，明细记录新旧值对比。 */
    @PutMapping("/modify/{id}")
    public Result<?> modify(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> row = anyById("selections", id);
        Result<?> deny = ownedOrDeny(row, "选科记录不存在", "无权操作他人选科");
        if (deny != null) return deny;
        String first = str(body.get("firstSubject")).trim();
        String second = str(body.get("secondSubjects")).trim();
        String note = str(body.get("note"));
        if (first.isBlank() || second.isBlank()) return Result.fail(400, "首选 / 再选科目必填");

        Map<String, Object> combo = anyById("subject-combinations", asLong(row.get("combinationId")));
        String oldFirst = combo == null ? "" : str(combo.get("firstSubject"));
        String oldSecond = combo == null ? "" : str(combo.get("secondSubjects"));
        String oldNote = str(row.get("note"));
        if (combo != null) {
            Map<String, Object> comboUp = new LinkedHashMap<>(combo);
            comboUp.put("firstSubject", first);
            comboUp.put("secondSubjects", second);
            comboUp.put("label", first + "+" + second);
            service.save("subject-combinations", comboUp);
        }
        Map<String, Object> up = new LinkedHashMap<>(row);
        up.put("status", "PENDING");
        up.put("confirmedAt", "");
        up.put("note", note);
        Object saved = service.save("selections", up);
        writeHistory(id, "MODIFY", "修改选科：组合 [" + oldFirst + "+" + oldSecond + "] → ["
                + first + "+" + second + "]；理由 [" + oldNote + "] → [" + note + "]");
        return Result.ok(toMap(saved));
    }

    /** 退选：状态改 CANCELLED，原因写入历史明细。 */
    @DeleteMapping("/cancel/{id}")
    public Result<?> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        Map<String, Object> row = anyById("selections", id);
        Result<?> deny = ownedOrDeny(row, "选科记录不存在", "无权操作他人选科");
        if (deny != null) return deny;
        Map<String, Object> up = new LinkedHashMap<>(row);
        up.put("status", "CANCELLED");
        Object saved = service.save("selections", up);
        writeHistory(id, "CANCEL", "退选，原因：" + (reason == null || reason.isBlank() ? "未填写" : reason));
        return Result.ok(toMap(saved));
    }

    // ============================================================ 选科意向

    @GetMapping("/intentions")
    public Result<?> intentions() { return Result.ok(mine("intentions")); }

    @PostMapping("/intentions")
    public Result<?> addIntention(@RequestBody Map<String, Object> body) {
        String major = str(body.get("targetMajor")).trim();
        String school = str(body.get("targetSchool")).trim();
        if (major.isBlank() && school.isBlank()) return Result.fail(400, "目标专业或目标院校至少填写一项");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("targetMajor", major);
        m.put("targetSchool", school);
        m.put("reason", str(body.get("reason")));
        String status = str(body.get("status"));
        m.put("status", status.isBlank() ? "DRAFT" : status);
        m.put("createdAt", now());
        return Result.ok(toMap(service.save("intentions", m)));
    }

    @PutMapping("/intentions/{id}/status")
    public Result<?> intentionStatus(@PathVariable Long id,
                                     @RequestParam(required = false) String status,
                                     @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> row = anyById("intentions", id);
        Result<?> deny = ownedOrDeny(row, "意向不存在", "无权操作他人意向");
        if (deny != null) return deny;
        String s = status != null && !status.isBlank() ? status : (body == null ? "" : str(body.get("status")));
        if (!INTENTION_STATUS.contains(s)) return Result.fail(400, "状态非法，可选 DRAFT / EVALUATED / CONFIRMED / ADOPTED");
        Map<String, Object> up = new LinkedHashMap<>(row);
        up.put("status", s);
        return Result.ok(toMap(service.save("intentions", up)));
    }

    // ============================================================ 选科指导（SWOT 拼入 content）

    @GetMapping("/guidances")
    public Result<?> guidances() { return Result.ok(mine("guidances")); }

    @PostMapping("/guidances")
    public Result<?> addGuidance(@RequestBody Map<String, Object> body) {
        String title = str(body.get("title")).trim();
        if (title.isBlank()) return Result.fail(400, "标题必填");
        @SuppressWarnings("unchecked")
        Map<String, Object> sw = body.get("swot") instanceof Map ? (Map<String, Object>) body.get("swot") : body;
        StringBuilder content = new StringBuilder(str(body.get("content")));
        appendSwot(content, "优势(S)", pick(sw.get("strength"), sw.get("S"), sw.get("s")));
        appendSwot(content, "劣势(W)", pick(sw.get("weakness"), sw.get("W"), sw.get("w")));
        appendSwot(content, "机会(O)", pick(sw.get("opportunity"), sw.get("O"), sw.get("o")));
        appendSwot(content, "威胁(T)", pick(sw.get("threat"), sw.get("T"), sw.get("t")));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("content", content.toString());
        String status = str(body.get("status"));
        m.put("status", status.isBlank() ? "ONGOING" : status);
        m.put("createdAt", now());
        return Result.ok(toMap(service.save("guidances", m)));
    }

    @PutMapping("/guidances/{id}/status")
    public Result<?> guidanceStatus(@PathVariable Long id,
                                    @RequestParam(required = false) String status,
                                    @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> row = anyById("guidances", id);
        Result<?> deny = ownedOrDeny(row, "指导记录不存在", "无权操作他人指导记录");
        if (deny != null) return deny;
        String s = status != null && !status.isBlank() ? status : (body == null ? "" : str(body.get("status")));
        if (!GUIDANCE_STATUS.contains(s)) return Result.fail(400, "状态非法，可选 ONGOING / COMPLETED / ADOPTED");
        Map<String, Object> up = new LinkedHashMap<>(row);
        up.put("status", s);
        return Result.ok(toMap(service.save("guidances", up)));
    }

    // ============================================================ 变更历史

    @GetMapping("/histories")
    public Result<?> histories(@RequestParam(required = false) String action,
                               @RequestParam(required = false, defaultValue = "100") int limit) {
        List<Map<String, Object>> list = mine("selection-histories");
        if (action != null && !action.isBlank()) {
            String a = action;
            list.removeIf(h -> !a.equalsIgnoreCase(str(h.get("action"))));
        }
        list.sort((x, y) -> str(y.get("createdAt")).compareTo(str(x.get("createdAt"))));
        if (limit > 0 && list.size() > limit) list = new ArrayList<>(list.subList(0, limit));
        return Result.ok(list);
    }

    /** 历史统计：total / pending / approved / rejected / confirmed（按当前账号选科状态聚合）。 */
    @GetMapping("/histories/statistics")
    public Result<?> historyStatistics() {
        List<Map<String, Object>> sels = mine("selections");
        int pending = 0, approved = 0, rejected = 0, confirmed = 0;
        for (Map<String, Object> s : sels) {
            String st = str(s.get("status")).toUpperCase(Locale.ROOT);
            if ("PENDING".equals(st)) pending++;
            else if ("APPROVED".equals(st)) approved++;
            else if ("REJECTED".equals(st)) rejected++;
            else if ("CONFIRMED".equals(st)) confirmed++;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", sels.size());
        out.put("pending", pending);
        out.put("approved", approved);
        out.put("rejected", rejected);
        out.put("confirmed", confirmed);
        return Result.ok(out);
    }

    // ============================================================ 审批（ADMIN）

    @GetMapping("/approvals/pending")
    public Result<?> pendingApprovals() {
        if (!AuthContext.isAdmin()) return Result.fail(403, "需要管理员权限");
        Map<Long, Map<String, Object>> combos = comboIndex();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> s : service.rows("selections")) {
            if (!"PENDING".equalsIgnoreCase(str(s.get("status")))) continue;
            Map<String, Object> row = new LinkedHashMap<>(s);
            Map<String, Object> combo = combos.get(asLong(s.get("combinationId")));
            row.put("firstSubject", combo == null ? null : combo.get("firstSubject"));
            row.put("secondSubjects", combo == null ? null : combo.get("secondSubjects"));
            row.put("label", combo == null ? null : combo.get("label"));
            out.add(row);
        }
        return Result.ok(out);
    }

    /** 真状态机：仅 PENDING 可处理；写选科历史(归属选科本人) + audit_log。 */
    @PutMapping("/approvals/{id}")
    public Result<?> approve(@PathVariable Long id,
                             @RequestParam(required = false) String decision,
                             @RequestParam(required = false) String comment) {
        if (!AuthContext.isAdmin()) return Result.fail(403, "需要管理员权限");
        String d = decision == null ? "" : decision.toUpperCase(Locale.ROOT);
        if (!"APPROVE".equals(d) && !"REJECT".equals(d)) return Result.fail(400, "decision 只能是 APPROVE 或 REJECT");
        Map<String, Object> row = anyById("selections", id);
        if (row == null) return Result.fail(404, "选科申请不存在");
        if (!"PENDING".equalsIgnoreCase(str(row.get("status"))))
            return Result.fail(409, "仅 PENDING 申请可处理，当前状态：" + str(row.get("status")));

        boolean pass = "APPROVE".equals(d);
        String newStatus = pass ? "APPROVED" : "REJECTED";
        String action = pass ? "APPROVE" : "REJECT";
        String auditAction = pass ? "APPROVE_SELECTION" : "REJECT_SELECTION";
        Map<String, Object> up = new LinkedHashMap<>(row);
        up.put("status", newStatus);
        Object saved = service.save("selections", up);

        String text = (pass ? "审批通过" : "审批驳回") + (comment == null || comment.isBlank() ? "" : "；意见：" + comment);
        String ts = now();
        // 审批历史归属选科本人，保证其在「变更历史」中可见；audit_log 为管理员操作留痕。
        jdbc.update("INSERT INTO selection_history (user_id, selection_id, action, operator, detail, created_at) "
                        + "VALUES (?,?,?,?,?,?)",
                asLong(row.get("userId")), id, action, operator(), text, ts);
        jdbc.update("INSERT INTO audit_log (operator_id, operator_name, action, target_type, target_id, "
                        + "before_value, after_value, created_at) VALUES (?,?,?,?,?,?,?,?)",
                AuthContext.userId(), operator(), auditAction, "course_selection", String.valueOf(id),
                "PENDING", newStatus, ts);
        return Result.ok(toMap(saved));
    }

    // ============================================================ 多维统计

    /** 热门组合 TOP5 / 首选科目分布 / 按月提交趋势 / 状态分布，全部真实聚合。
     *  注：旧 PlanningFeatureController 已占用 GET /api/v1/planning/selection/statistics，
     *  组合后路径与本方法完全相同。此处加一个 !legacy 参数条件，既区分两个 RequestMappingInfo
     *  避免启动期 "Ambiguous mapping"，又因多一个 params 条件使本映射匹配优先级更高。 */
    @GetMapping(value = "/statistics", params = "!legacy")
    public Result<?> statistics() {
        List<Map<String, Object>> sels = mine("selections");
        Map<Long, Map<String, Object>> combos = comboIndex();
        Map<String, Integer> comboCount = new TreeMap<>();
        Map<String, Integer> firstDist = new TreeMap<>();
        Map<String, Integer> monthly = new TreeMap<>();
        Map<String, Integer> statusDist = new TreeMap<>();
        for (Map<String, Object> s : sels) {
            Map<String, Object> c = combos.get(asLong(s.get("combinationId")));
            comboCount.merge(comboLabel(c), 1, Integer::sum);
            firstDist.merge(c == null ? "未知" : unknownIfBlank(str(c.get("firstSubject"))), 1, Integer::sum);
            String m = monthOf(str(s.get("createdAt")));
            if (!m.isEmpty()) monthly.merge(m, 1, Integer::sum);
            statusDist.merge(unknownIfBlank(str(s.get("status"))), 1, Integer::sum);
        }
        List<Map<String, Object>> hot = comboCount.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("label", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toCollection(ArrayList::new));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", sels.size());
        out.put("hotCombinations", hot);
        out.put("firstSubjectDistribution", firstDist);
        out.put("monthlyTrend", monthly);
        out.put("statusDistribution", statusDist);
        return Result.ok(out);
    }

    // ============================================================ 智能建议（真实：成绩 + 专业要求）

    /** 依据 score_record 各科平均分找出薄弱/强项学科，结合 major_requirement.required_subjects 给出结构化组合建议。 */
    @GetMapping("/advice")
    public Result<?> advice() {
        Long id = uid();
        Map<String, List<Double>> bySubject = new LinkedHashMap<>();
        if (id != null) {
            try {
                for (Map<String, Object> r : queryLower("SELECT subject, score FROM score_record WHERE user_id = ?", id)) {
                    String subj = str(r.get("subject"));
                    if (subj.isBlank()) continue;
                    bySubject.computeIfAbsent(subj, k -> new ArrayList<>()).add(num(r.get("score")));
                }
            } catch (RuntimeException ignored) { /* 成绩表暂不可用时按无数据处理 */ }
        }
        Map<String, Double> avgBySubject = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> e : bySubject.entrySet()) {
            avgBySubject.put(e.getKey(), round(e.getValue().stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0)));
        }
        double overall = avgBySubject.isEmpty() ? 0 : round(avgBySubject.values().stream()
                .mapToDouble(Double::doubleValue).average().orElse(0));
        Set<String> weak = new HashSet<>(), strong = new HashSet<>();
        avgBySubject.forEach((subj, avg) -> {
            if (avg < overall) weak.add(subj); else strong.add(subj);
        });

        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("type", "SUMMARY");
        summary.put("scoreCount", avgBySubject.size());
        summary.put("overallAverage", overall);
        List<Map<String, Object>> weakList = new ArrayList<>();
        avgBySubject.forEach((subj, avg) -> {
            if (weak.contains(subj)) {
                Map<String, Object> w = new LinkedHashMap<>();
                w.put("subject", subj);
                w.put("avgScore", avg);
                weakList.add(w);
            }
        });
        summary.put("weakSubjects", weakList);
        summary.put("strongSubjects", new ArrayList<>(strong));
        result.add(summary);

        List<Map<String, Object>> majors = service.rows("major-requirements");
        // 学科需求度：统计每个学科被多少个专业的 required_subjects 要求。
        Map<String, Integer> demand = new TreeMap<>();
        for (Map<String, Object> m : majors) {
            for (String r : splitSubjects(str(m.get("requiredSubjects")))) demand.merge(r, 1, Integer::sum);
        }
        // 组合建议：按需求度排序，优先规避薄弱学科。
        List<String> ranked = new ArrayList<>(demand.keySet());
        ranked.sort((a, b) -> {
            int c = Integer.compare(demand.getOrDefault(b, 0), demand.getOrDefault(a, 0));
            return c != 0 ? c : a.compareTo(b);
        });
        List<String> preferred = ranked.stream().filter(s -> !weak.contains(s))
                .collect(Collectors.toCollection(ArrayList::new));
        Set<String> comboSet = new HashSet<>(preferred);
        int covered = 0;
        for (Map<String, Object> m : majors) {
            List<String> req = splitSubjects(str(m.get("requiredSubjects")));
            if (!req.isEmpty() && comboSet.containsAll(req)) covered++;
        }
        Map<String, Object> combo = new LinkedHashMap<>();
        combo.put("type", "COMBINATION");
        combo.put("firstSubject", preferred.isEmpty() ? "" : preferred.get(0));
        combo.put("secondSubjects", preferred.size() > 1
                ? String.join(",", preferred.subList(1, Math.min(preferred.size(), 4))) : "");
        combo.put("weakSubjectsToAvoid", new ArrayList<>(weak));
        combo.put("subjectDemand", demand);
        combo.put("coveredMajorCount", covered);
        combo.put("candidateCount", preferred.size());
        result.add(combo);

        // 逐专业建议：全部专业都参与，按匹配分降序、薄弱风险升序。
        List<Map<String, Object>> suggestions = new ArrayList<>();
        for (Map<String, Object> major : majors) {
            List<String> req = splitSubjects(str(major.get("requiredSubjects")));
            if (req.isEmpty()) continue;
            List<String> strongHits = new ArrayList<>(), weakRisks = new ArrayList<>();
            List<String> safe = new ArrayList<>();
            for (String r : req) {
                if (strong.contains(r)) strongHits.add(r);
                if (weak.contains(r)) weakRisks.add(r); else safe.add(r);
            }
            List<String> base = strongHits.isEmpty() ? (safe.isEmpty() ? req : safe) : strongHits;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "SUGGESTION");
            item.put("majorName", major.get("majorName"));
            item.put("majorCode", major.get("majorCode"));
            item.put("requiredSubjects", req);
            item.put("strongSubjects", strongHits);
            item.put("weakSubjects", weakRisks);
            item.put("hitCount", strongHits.size());
            item.put("requiredCount", req.size());
            item.put("riskCount", weakRisks.size());
            item.put("matchingScore", round((double) strongHits.size() / req.size()));
            item.put("recommendedFirstSubject", base.get(0));
            item.put("recommendedSecondSubjects", base.size() > 1
                    ? String.join(",", base.subList(1, base.size())) : "");
            suggestions.add(item);
        }
        suggestions.sort((a, b) -> {
            int c = Double.compare(num(b.get("matchingScore")), num(a.get("matchingScore")));
            if (c != 0) return c;
            return Double.compare(num(a.get("riskCount")), num(b.get("riskCount")));
        });
        result.addAll(suggestions.subList(0, Math.min(suggestions.size(), 12)));
        return Result.ok(result);
    }

    // ============================================================ 学科 / 专业匹配

    /** {first:[...], second:[...]}，取自 subject_option.is_first 标记。 */
    @GetMapping("/subjects")
    public Result<?> subjects() {
        List<String> first = new ArrayList<>(), second = new ArrayList<>();
        for (Map<String, Object> o : service.rows("subject-options")) {
            String name = str(o.get("name"));
            if (name.isBlank()) continue;
            if (truthy(pick(o.get("isFirst"), o.get("is_first"), o.get("first")))) first.add(name);
            else second.add(name);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("first", first);
        out.put("second", second);
        return Result.ok(out);
    }

    /** 真实选科匹配：required_subjects 按逗号拆开，命中率 = 命中数 / 要求数，降序返回。 */
    @GetMapping("/major-match")
    public Result<?> majorMatch(@RequestParam(required = false) String firstSubject,
                                @RequestParam(required = false) String subjects) {
        Set<String> selected = new LinkedHashSet<>();
        if (firstSubject != null && !firstSubject.isBlank()) selected.add(firstSubject.trim());
        selected.addAll(splitSubjects(subjects));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> m : service.rows("major-requirements")) {
            List<String> req = splitSubjects(str(m.get("requiredSubjects")));
            List<String> hits = new ArrayList<>();
            for (String r : req) if (selected.contains(r)) hits.add(r);
            Map<String, Object> row = new LinkedHashMap<>(m);
            row.put("requiredList", req);
            row.put("matchedSubjects", hits);
            row.put("hitCount", hits.size());
            row.put("requiredCount", req.size());
            row.put("matchRate", req.isEmpty() ? 0.0 : round((double) hits.size() / req.size()));
            out.add(row);
        }
        out.sort((a, b) -> Double.compare(num(b.get("matchRate")), num(a.get("matchRate"))));
        return Result.ok(out);
    }

    // ============================================================ helpers

    private Long uid() { return AuthContext.userId(); }

    /** 账号隔离：仅返回当前账号的数据。rows() 返回全量，需按 userId 过滤。 */
    private List<Map<String, Object>> mine(String resource) {
        Long id = uid();
        if (id == null) return new ArrayList<>();
        return service.rows(resource).stream()
                .filter(r -> id.equals(asLong(r.get("userId"))))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /** 跨账号按 id 精确查找（用于归属判定，不直接返回数据）。 */
    private Map<String, Object> anyById(String resource, Long id) {
        if (id == null) return null;
        return service.rows(resource).stream()
                .filter(r -> id.equals(asLong(r.get("id"))))
                .findFirst().orElse(null);
    }

    /** 归属校验：不存在→404，存在但不属于当前账号→403；通过返回 null。 */
    private Result<?> ownedOrDeny(Map<String, Object> row, String missing, String forbidden) {
        if (row == null) return Result.fail(404, missing);
        if (!Objects.equals(uid(), asLong(row.get("userId")))) return Result.fail(403, forbidden);
        return null;
    }

    private Map<Long, Map<String, Object>> comboIndex() {
        Map<Long, Map<String, Object>> map = new HashMap<>();
        for (Map<String, Object> c : service.rows("subject-combinations")) map.put(asLong(c.get("id")), c);
        return map;
    }

    private void writeHistory(Long selectionId, String action, String detail) {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("selectionId", selectionId);
        h.put("action", action);
        h.put("operator", operator());
        h.put("detail", detail);
        h.put("createdAt", now());
        service.save("selection-histories", h);
    }

    /** 原生 SQL 结果列名统一小写，兼容 MySQL / H2 对列标签大小写的差异。 */
    private List<Map<String, Object>> queryLower(String sql, Object... args) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList(sql, args)) {
            Map<String, Object> m = new LinkedHashMap<>();
            row.forEach((k, v) -> m.put(k.toLowerCase(Locale.ROOT), v));
            out.add(m);
        }
        return out;
    }

    private static String comboLabel(Map<String, Object> c) {
        if (c == null) return "未知组合";
        String label = str(c.get("label"));
        if (!label.isBlank()) return label;
        String f = str(c.get("firstSubject")), s = str(c.get("secondSubjects"));
        String v = (f + "+" + s).replaceAll("\\+$", "");
        return v.isBlank() ? "未知组合" : v;
    }

    private static void appendSwot(StringBuilder sb, String label, Object value) {
        String v = str(value);
        if (!v.isBlank()) {
            if (sb.length() > 0) sb.append('\n');
            sb.append('【').append(label).append('】').append(v);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object o) { return json.convertValue(o, Map.class); }

    private static List<String> splitSubjects(String s) {
        if (s == null || s.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        for (String p : s.split("[,，、/;；\\s]+")) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static String pick(Object... vals) {
        for (Object v : vals) if (v != null && !String.valueOf(v).isBlank()) return String.valueOf(v);
        return "";
    }

    private static boolean truthy(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        String s = String.valueOf(v);
        return "true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s);
    }

    private static String unknownIfBlank(String s) { return s.isBlank() ? "未知" : s; }

    private static String monthOf(String s) { return s == null || s.length() < 7 ? "" : s.substring(0, 7); }

    private static double num(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        if (o == null) return 0;
        try { return Double.parseDouble(String.valueOf(o)); } catch (NumberFormatException e) { return 0; }
    }

    private static Long asLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o == null) return null;
        try { return Long.valueOf(String.valueOf(o).trim()); } catch (NumberFormatException e) { return null; }
    }

    private static long lid(Map<String, Object> m) { Long v = asLong(m.get("id")); return v == null ? 0L : v; }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }

    private static String operator() { String u = AuthContext.username(); return u == null || u.isBlank() ? "self" : u; }

    private static String now() { return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
}
