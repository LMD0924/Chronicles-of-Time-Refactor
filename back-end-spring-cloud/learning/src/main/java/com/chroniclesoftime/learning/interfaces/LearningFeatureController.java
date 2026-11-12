package com.chroniclesoftime.learning.interfaces;

import com.chroniclesoftime.learning.application.LearningApplicationService;
import com.chroniclesoftime.shared.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 学习域特性：练习判分、题目字典、成绩趋势、薄弱学科、知识图谱与学习路径。
 * 所有读取默认按当前账号隔离；管理员显式 all=true 时可跨账号核查。
 */
@RestController
@RequestMapping("/api/v1/learning")
public class LearningFeatureController {
    private final LearningApplicationService service;
    public LearningFeatureController(LearningApplicationService service) { this.service = service; }

    /** 题库筛选字典：学科 / 知识点 / 难度 / 来源，均由真实数据推导。 */
    @GetMapping("/questions/facets")
    public Result<?> facets(@RequestParam(defaultValue = "false") boolean all) {
        Set<String> subjects = new TreeSet<>(), points = new TreeSet<>(), diffs = new TreeSet<>(), sources = new TreeSet<>();
        for (Map<String, Object> q : service.rowsScoped("questions", all)) {
            add(subjects, q.get("subject")); add(points, q.get("knowledgePoint"));
            add(diffs, q.get("difficulty")); add(sources, q.get("source"));
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("subjects", subjects); m.put("knowledgePoints", points);
        m.put("difficulties", diffs); m.put("sources", sources);
        return Result.ok(m);
    }

    /** 学习总览指标。 */
    @GetMapping("/stats")
    public Result<?> stats(@RequestParam(defaultValue = "false") boolean all) {
        List<Map<String, Object>> answers = service.rowsScoped("answers", all);
        long correct = answers.stream().filter(a -> Boolean.TRUE.equals(a.get("correct"))).count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("questions", service.rowsScoped("questions", all).size());
        m.put("answers", answers.size());
        m.put("correct", correct);
        m.put("accuracy", answers.isEmpty() ? 0 : Math.round(correct * 1000.0 / answers.size()) / 10.0);
        m.put("sessions", service.rowsScoped("sessions", all).size());
        m.put("mistakes", service.rowsScoped("mistakes", all).size());
        m.put("pendingMistakes", service.rowsScoped("mistakes", all).stream()
                .filter(x -> !Boolean.TRUE.equals(x.get("mastered"))).count());
        return Result.ok(m);
    }

    @GetMapping("/random-questions")
    public Result<?> random(@RequestParam(defaultValue = "10") int size,
                            @RequestParam(required = false) String subject,
                            @RequestParam(required = false) String knowledgePoint,
                            @RequestParam(required = false) String difficulty,
                            @RequestParam(defaultValue = "false") boolean all) {
        List<Map<String, Object>> qs = new ArrayList<>();
        for (Map<String, Object> q : service.rowsScoped("questions", all)) {
            if (subject != null && !subject.isBlank() && !subject.equals(text(q.get("subject")))) continue;
            if (knowledgePoint != null && !knowledgePoint.isBlank() && !knowledgePoint.equals(text(q.get("knowledgePoint")))) continue;
            if (difficulty != null && !difficulty.isBlank() && !difficulty.equals(text(q.get("difficulty")))) continue;
            qs.add(q);
        }
        Collections.shuffle(qs);
        return Result.ok(qs.subList(0, Math.min(Math.max(size, 1), qs.size())));
    }

    /** 兼容旧单题提交：可带 sessionId 归入场次；错题按 question_id 去重并累加复习次数。 */
    @PostMapping("/practice/submit")
    public Result<?> submit(@RequestBody Map<String, Object> body) {
        Long qid = longOf(body.get("questionId"));
        if (qid == null) return Result.fail(400, "questionId 不能为空");
        Map<String, Object> q = service.rowsScoped("questions", false).stream()
                .filter(x -> qid.equals(longOf(x.get("id")))).findFirst().orElse(null);
        if (q == null) return Result.fail(404, "题目不存在");
        String answer = text(body.get("userAnswer"));
        boolean correct = answer.trim().equalsIgnoreCase(text(q.get("answer")).trim());
        String now = now();

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("questionId", qid); record.put("userAnswer", answer);
        record.put("correct", correct); record.put("score", correct ? 100 : 0);
        record.put("durationMs", body.getOrDefault("durationMs", 0));
        record.put("answeredAt", now);
        if (body.get("sessionId") != null) record.put("sessionId", longOf(body.get("sessionId")));
        Object saved = service.save("answers", record);
        if (!correct) service.upsertMistake(q, now);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("correct", correct); out.put("answer", q.get("answer"));
        out.put("explanation", q.get("explanation")); out.put("record", saved);
        return Result.ok(out);
    }

    @GetMapping("/knowledge-graph")
    public Result<?> knowledgeGraph(@RequestParam(defaultValue = "false") boolean all) {
        Map<String, Map<String, Object>> nodes = new LinkedHashMap<>();
        for (Map<String, Object> q : service.rowsScoped("questions", all)) {
            String kp = text(q.get("knowledgePoint")).isBlank() ? "未分类" : text(q.get("knowledgePoint"));
            Map<String, Object> n = nodes.computeIfAbsent(kp, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", k); m.put("name", k); m.put("value", 0); m.put("subject", q.get("subject"));
                return m;
            });
            n.put("value", (n.get("value") instanceof Number v ? v.intValue() : 0) + 1);
        }
        long mistakes = service.rowsScoped("mistakes", all).stream()
                .filter(m -> !Boolean.TRUE.equals(m.get("mastered"))).count();
        List<Map<String, Object>> edges = new ArrayList<>();
        List<String> names = new ArrayList<>(nodes.keySet());
        for (int i = 0; i < names.size(); i++) {
            for (int j = i + 1; j < names.size(); j++) {
                if (Objects.equals(nodes.get(names.get(i)).get("subject"), nodes.get(names.get(j)).get("subject"))) {
                    edges.add(Map.of("source", names.get(i), "target", names.get(j), "weight", 1));
                }
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("nodes", new ArrayList<>(nodes.values()));
        out.put("edges", edges);
        out.put("pendingMistakes", mistakes);
        return Result.ok(out);
    }

    @GetMapping("/heatmap")
    public Result<?> heatmap(@RequestParam(defaultValue = "false") boolean all) {
        Map<String, Integer> m = new TreeMap<>();
        for (Map<String, Object> a : service.rowsScoped("answers", all)) {
            String d = text(a.get("answeredAt"));
            if (d.isBlank()) continue;
            m.merge(d.substring(0, Math.min(10, d.length())), 1, Integer::sum);
        }
        return Result.ok(m);
    }

    /** 学习路径：按知识点的答题正确率给出掌握度，薄弱点优先。 */
    @GetMapping("/path")
    public Result<?> path(@RequestParam(defaultValue = "false") boolean all) {
        Map<Long, Map<String, Object>> questions = new HashMap<>();
        for (Map<String, Object> q : service.rowsScoped("questions", all)) {
            if (q.get("id") instanceof Number n) questions.put(n.longValue(), q);
        }
        Map<String, int[]> byPoint = new LinkedHashMap<>();
        for (Map<String, Object> a : service.rowsScoped("answers", all)) {
            Map<String, Object> q = questions.get(longOf(a.get("questionId")));
            if (q == null) continue;
            String kp = text(q.get("knowledgePoint")).isBlank() ? "未分类" : text(q.get("knowledgePoint"));
            int[] acc = byPoint.computeIfAbsent(kp, k -> new int[]{0, 0});
            acc[0]++; if (Boolean.TRUE.equals(a.get("correct"))) acc[1]++;
        }
        for (Map<String, Object> m : service.rowsScoped("mistakes", all)) {
            String kp = text(m.get("knowledgePoint")).isBlank() ? "未分类" : text(m.get("knowledgePoint"));
            byPoint.computeIfAbsent(kp, k -> new int[]{0, 0});
        }
        List<Map<String, Object>> out = new ArrayList<>();
        byPoint.forEach((kp, v) -> {
            double mastery = v[0] == 0 ? 0 : Math.round(v[1] * 1000.0 / v[0]) / 10.0;
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("knowledgePoint", kp); p.put("mastery", mastery);
            p.put("attempts", v[0]); p.put("correct", v[1]);
            p.put("suggested", mastery < 60);
            out.add(p);
        });
        out.sort(Comparator.comparingDouble(m -> (Double) m.get("mastery")));
        return Result.ok(out);
    }

    @GetMapping("/weak-subjects")
    public Result<?> weakSubjects(@RequestParam(defaultValue = "false") boolean all) {
        Map<String, List<Double>> bySubject = new LinkedHashMap<>();
        for (Map<String, Object> s : service.rowsScoped("scores", all)) {
            bySubject.computeIfAbsent(text(s.get("subject")), k -> new ArrayList<>()).add(num(s.get("score")));
        }
        List<Map<String, Object>> out = new ArrayList<>();
        bySubject.forEach((k, v) -> out.add(Map.of("subject", k,
                "average", v.stream().mapToDouble(Double::doubleValue).average().orElse(0))));
        out.sort(Comparator.comparingDouble(m -> (Double) m.get("average")));
        return Result.ok(out);
    }

    @GetMapping("/trend")
    public Result<?> trend(@RequestParam(defaultValue = "false") boolean all) {
        Map<String, List<Double>> byExam = new TreeMap<>();
        for (Map<String, Object> s : service.rowsScoped("scores", all)) {
            byExam.computeIfAbsent(text(s.get("examName")), k -> new ArrayList<>()).add(num(s.get("score")));
        }
        List<Map<String, Object>> out = new ArrayList<>();
        byExam.forEach((k, v) -> out.add(Map.of("exam", k,
                "average", v.stream().mapToDouble(Double::doubleValue).average().orElse(0))));
        return Result.ok(out);
    }

    @GetMapping("/radar")
    public Result<?> radar(@RequestParam(defaultValue = "false") boolean all) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (Map<String, Object> s : service.rowsScoped("scores", all)) {
            m.merge(text(s.get("subject")), num(s.get("score")), (a, b) -> (a + b) / 2);
        }
        return Result.ok(m);
    }

    static String now() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    static String text(Object v) { return v == null ? "" : String.valueOf(v); }
    static Long longOf(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o == null || String.valueOf(o).isBlank()) return null;
        try { return Long.parseLong(String.valueOf(o)); } catch (NumberFormatException e) { return null; }
    }
    static void add(Set<String> set, Object v) {
        String s = text(v).trim();
        if (!s.isBlank()) set.add(s);
    }
    static double num(Object o) { return o instanceof Number n ? n.doubleValue() : 0; }
}
