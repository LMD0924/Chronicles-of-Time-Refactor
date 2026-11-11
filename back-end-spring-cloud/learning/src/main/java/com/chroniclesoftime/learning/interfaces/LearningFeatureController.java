package com.chroniclesoftime.learning.interfaces;

import com.chroniclesoftime.learning.application.LearningApplicationService;
import com.chroniclesoftime.shared.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** 学习域特性：练习判分、错题本、成绩趋势、薄弱学科、知识图谱与学习路径。 */
@RestController
@RequestMapping("/api/v1/learning")
public class LearningFeatureController {
    private final LearningApplicationService service;
    public LearningFeatureController(LearningApplicationService service) { this.service = service; }

    @GetMapping("/knowledge-graph")
    public Result<?> knowledgeGraph() {
        Map<String, Map<String, Object>> nodes = new LinkedHashMap<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        for (Map<String, Object> q : service.rows("questions")) {
            String kp = String.valueOf(q.getOrDefault("knowledgePoint", "未分类"));
            Map<String, Object> n = nodes.computeIfAbsent(kp, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", k); m.put("name", k); m.put("value", 0); m.put("category", q.get("subject"));
                return m;
            });
            n.put("value", ((Number) n.get("value")).intValue() + 1);
        }
        for (Map<String, Object> m : service.rows("mistakes")) {
            String kp = String.valueOf(m.getOrDefault("knowledgePoint", "未分类"));
            edges.add(Map.of("source", kp, "target", String.valueOf(m.getOrDefault("reason", "易错")), "weight", m.getOrDefault("reviewCount", 1)));
        }
        return Result.ok(Map.of("nodes", new ArrayList<>(nodes.values()), "edges", edges));
    }

    @GetMapping("/heatmap")
    public Result<?> heatmap() {
        Map<String, Integer> m = new TreeMap<>();
        for (Map<String, Object> a : service.rows("answers")) {
            String d = String.valueOf(a.getOrDefault("answeredAt", "")).substring(0, Math.min(10, String.valueOf(a.getOrDefault("answeredAt", "")).length()));
            m.merge(d, 1, Integer::sum);
        }
        return Result.ok(m);
    }

    @GetMapping("/path")
    public Result<?> path() {
        Map<String, Double> mastery = new LinkedHashMap<>();
        for (Map<String, Object> m : service.rows("mistakes")) {
            String kp = String.valueOf(m.getOrDefault("knowledgePoint", "未分类"));
            double v = Boolean.TRUE.equals(m.get("mastered")) ? 1.0 : 0.35;
            mastery.merge(kp, v, Double::sum);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        mastery.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(e -> {
            out.add(Map.of("knowledgePoint", e.getKey(), "mastery", e.getValue(), "suggested", e.getValue() < 1));
        });
        return Result.ok(out);
    }

    @GetMapping("/weak-subjects")
    public Result<?> weakSubjects() {
        Map<String, List<Double>> bySubject = new LinkedHashMap<>();
        for (Map<String, Object> s : service.rows("scores")) {
            bySubject.computeIfAbsent(String.valueOf(s.get("subject")), k -> new ArrayList<>()).add(num(s.get("score")));
        }
        List<Map<String, Object>> out = new ArrayList<>();
        bySubject.forEach((k, v) -> out.add(Map.of("subject", k, "average", v.stream().mapToDouble(Double::doubleValue).average().orElse(0))));
        out.sort(Comparator.comparingDouble(m -> (Double) m.get("average")));
        return Result.ok(out);
    }

    @GetMapping("/trend")
    public Result<?> trend() {
        Map<String, List<Double>> byExam = new TreeMap<>();
        for (Map<String, Object> s : service.rows("scores")) {
            byExam.computeIfAbsent(String.valueOf(s.getOrDefault("examName", "")), k -> new ArrayList<>()).add(num(s.get("score")));
        }
        List<Map<String, Object>> out = new ArrayList<>();
        byExam.forEach((k, v) -> out.add(Map.of("exam", k, "average", v.stream().mapToDouble(Double::doubleValue).average().orElse(0))));
        return Result.ok(out);
    }

    @GetMapping("/radar")
    public Result<?> radar() {
        Map<String, Double> m = new LinkedHashMap<>();
        for (Map<String, Object> s : service.rows("scores")) {
            m.merge(String.valueOf(s.get("subject")), num(s.get("score")), (a, b) -> (a + b) / 2);
        }
        return Result.ok(m);
    }

    @GetMapping("/random-questions")
    public Result<?> random(@RequestParam(defaultValue = "10") int size) {
        List<Map<String, Object>> qs = new ArrayList<>(service.rows("questions"));
        Collections.shuffle(qs);
        return Result.ok(qs.subList(0, Math.min(size, qs.size())));
    }

    @PostMapping("/practice/submit")
    public Result<?> submit(@RequestBody Map<String, Object> body) {
        Long qid = Long.valueOf(String.valueOf(body.get("questionId")));
        String answer = String.valueOf(body.getOrDefault("userAnswer", ""));
        Map<String, Object> q = service.rows("questions").stream()
                .filter(x -> qid.equals(longOf(x.get("id")))).findFirst().orElse(null);
        if (q == null) return Result.fail(404, "题目不存在");
        boolean correct = answer.equalsIgnoreCase(String.valueOf(q.getOrDefault("answer", "")));
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("questionId", qid); record.put("userAnswer", answer);
        record.put("correct", correct); record.put("score", correct ? 100 : 0);
        record.put("durationMs", body.getOrDefault("durationMs", 0));
        record.put("answeredAt", java.time.LocalDateTime.now().toString().substring(0, 19).replace('T', ' '));
        Object saved = service.save("answers", record);
        if (!correct) {
            Map<String, Object> mistake = new LinkedHashMap<>();
            mistake.put("questionId", qid); mistake.put("knowledgePoint", q.get("knowledgePoint"));
            mistake.put("reason", "答案错误"); mistake.put("mastered", false); mistake.put("reviewCount", 1);
            mistake.put("createdAt", record.get("answeredAt"));
            service.save("mistakes", mistake);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("correct", correct); out.put("answer", q.get("answer"));
        out.put("explanation", q.get("explanation")); out.put("record", saved);
        return Result.ok(out);
    }

    private static double num(Object o) { return o instanceof Number n ? n.doubleValue() : 0; }
    private static Long longOf(Object o) { return o instanceof Number n ? n.longValue() : null; }
}
