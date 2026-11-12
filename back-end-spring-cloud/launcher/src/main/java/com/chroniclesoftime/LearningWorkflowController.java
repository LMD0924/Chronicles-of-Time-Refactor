package com.chroniclesoftime;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.learning.application.LearningApplicationService;
import com.chroniclesoftime.shared.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 学习完整链路工作流：组卷 → 逐题作答 → 批量判分 → 场次 / 答题记录 / 错题三处同步 → 回看与重练。
 * V6 已给 answer_records 增加 session_id，这里用该字段把场次与逐题记录精确关联。
 */
@RestController
@RequestMapping("/api/v1/learning")
public class LearningWorkflowController {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LearningApplicationService service;
    private final ObjectMapper json;

    public LearningWorkflowController(LearningApplicationService service, ObjectMapper json) {
        this.service = service;
        this.json = json;
    }

    /** 按学科 / 知识点 / 难度 / 来源组卷，创建练习场次；返回的题目不含答案与解析。 */
    @PostMapping("/practice/start")
    public Result<?> start(@RequestBody Map<String, Object> body) {
        String subject = text(body.get("subject"));
        String point = text(body.get("knowledgePoint"));
        String difficulty = text(body.get("difficulty"));
        String source = text(body.getOrDefault("source", "bank"));
        int size = Math.max(1, Math.min(intOf(body.get("size"), 10), 50));
        String now = now();
        String title = text(body.get("title"));
        if (title.isBlank()) title = ("mistakes".equals(source) ? "错题重练 " : "练习 ") + now;

        List<Map<String, Object>> pool = new ArrayList<>(service.rowsScoped("questions", false));
        if ("mistakes".equals(source)) {
            Set<Long> ids = new HashSet<>();
            for (Map<String, Object> m : service.rowsScoped("mistakes", false)) {
                if (!Boolean.TRUE.equals(m.get("mastered"))) {
                    Long qid = longOf(m.get("questionId"));
                    if (qid != null) ids.add(qid);
                }
            }
            pool.removeIf(q -> !ids.contains(longOf(q.get("id"))));
        }
        pool.removeIf(q -> !subject.isBlank() && !subject.equals(text(q.get("subject")))
                || !point.isBlank() && !point.equals(text(q.get("knowledgePoint")))
                || !difficulty.isBlank() && !difficulty.equals(text(q.get("difficulty"))));
        if (pool.isEmpty()) return Result.fail(400, "没有符合条件的题目，请先到题库管理添加或更换筛选条件");
        Collections.shuffle(pool);
        List<Map<String, Object>> picked = new ArrayList<>(pool.subList(0, Math.min(size, pool.size())));

        Map<String, Object> session = new LinkedHashMap<>();
        session.put("title", title);
        session.put("questionCount", picked.size());
        session.put("correctCount", 0);
        session.put("startedAt", now);
        Object saved = service.save("sessions", session);
        Long sid = longOf(toMap(saved).get("id"));
        if (sid == null) return Result.fail(500, "场次创建失败");

        List<Map<String, Object>> questions = new ArrayList<>();
        for (Map<String, Object> q : picked) {
            Map<String, Object> safe = new LinkedHashMap<>(q);
            safe.remove("answer");
            safe.remove("explanation");
            questions.add(safe);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sessionId", sid);
        out.put("title", title);
        out.put("startedAt", now);
        out.put("questions", questions);
        return Result.ok(out);
    }

    /** 一次性交卷：逐题判分、写答题记录（带 session_id）、自动积累错题、回填场次正确数。 */
    @PostMapping("/practice/{sessionId}/submit")
    public Result<?> submit(@PathVariable Long sessionId, @RequestBody Map<String, Object> body) {
        Map<String, Object> session = toMap(service.get("sessions", sessionId));
        if (session.isEmpty()) return Result.fail(404, "练习场次不存在");
        assertOwned(longOf(session.get("userId")));
        if (session.get("finishedAt") != null) return Result.fail(409, "该场次已交卷，不能重复提交");

        Map<Long, Map<String, Object>> questions = new HashMap<>();
        for (Map<String, Object> q : service.rowsScoped("questions", false)) {
            Long id = longOf(q.get("id"));
            if (id != null) questions.put(id, q);
        }
        Object raw = body.get("answers");
        List<?> incoming = raw instanceof List<?> l ? l : List.of();
        if (incoming.isEmpty()) return Result.fail(400, "没有可提交的答案");

        String now = now();
        int correctCount = 0;
        List<Map<String, Object>> results = new ArrayList<>();
        for (Object item : incoming) {
            if (!(item instanceof Map<?, ?> rawOne)) continue;
            @SuppressWarnings("unchecked") Map<String, Object> one = (Map<String, Object>) rawOne;
            Long qid = longOf(one.get("questionId"));
            Map<String, Object> q = questions.get(qid);
            if (q == null) continue;
            String given = text(one.get("userAnswer"));
            boolean correct = !given.isBlank() && given.trim().equalsIgnoreCase(text(q.get("answer")).trim());

            Map<String, Object> record = new LinkedHashMap<>();
            record.put("questionId", qid);
            record.put("userAnswer", given);
            record.put("correct", correct);
            record.put("score", correct ? 100 : 0);
            record.put("durationMs", one.getOrDefault("durationMs", 0));
            record.put("answeredAt", now);
            record.put("sessionId", sessionId);
            service.save("answers", record);
            if (!correct) service.upsertMistake(q, now); else correctCount++;

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("questionId", qid);
            r.put("title", q.get("title"));
            r.put("userAnswer", given);
            r.put("correct", correct);
            r.put("answer", q.get("answer"));
            r.put("explanation", q.get("explanation"));
            r.put("knowledgePoint", q.get("knowledgePoint"));
            results.add(r);
        }

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("id", sessionId);
        update.put("questionCount", results.size());
        update.put("correctCount", correctCount);
        update.put("finishedAt", now);
        service.save("sessions", update);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sessionId", sessionId);
        out.put("total", results.size());
        out.put("correctCount", correctCount);
        out.put("accuracy", results.isEmpty() ? 0 : Math.round(correctCount * 1000.0 / results.size()) / 10.0);
        out.put("results", results);
        return Result.ok(out);
    }

    @GetMapping("/practice/sessions")
    public Result<?> sessions() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> s : service.rowsScoped("sessions", false)) {
            Map<String, Object> one = new LinkedHashMap<>(s);
            int total = s.get("questionCount") instanceof Number n ? n.intValue() : 0;
            int correct = s.get("correctCount") instanceof Number n ? n.intValue() : 0;
            one.put("accuracy", total == 0 ? 0 : Math.round(correct * 1000.0 / total) / 10.0);
            one.put("finished", s.get("finishedAt") != null);
            out.add(one);
        }
        return Result.ok(out);
    }

    /** 场次回看：逐题带出题干、你的答案、正确答案与解析。 */
    @GetMapping("/practice/sessions/{id}")
    public Result<?> sessionDetail(@PathVariable Long id) {
        Map<String, Object> session = toMap(service.get("sessions", id));
        if (session.isEmpty()) return Result.fail(404, "练习场次不存在");
        assertOwned(longOf(session.get("userId")));
        Map<Long, Map<String, Object>> questions = new HashMap<>();
        for (Map<String, Object> q : service.rowsScoped("questions", false)) {
            Long qid = longOf(q.get("id"));
            if (qid != null) questions.put(qid, q);
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> a : service.findBy("answers", "session_id", id)) {
            Map<String, Object> one = new LinkedHashMap<>(a);
            Map<String, Object> q = questions.get(longOf(a.get("questionId")));
            if (q != null) {
                one.put("title", q.get("title")); one.put("content", q.get("content"));
                one.put("optionsJson", q.get("optionsJson")); one.put("answer", q.get("answer"));
                one.put("explanation", q.get("explanation")); one.put("subject", q.get("subject"));
                one.put("knowledgePoint", q.get("knowledgePoint"));
            }
            items.add(one);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("session", session);
        out.put("items", items);
        return Result.ok(out);
    }

    /** 批量导入题目（前端粘贴 JSON 数组）：逐条校验，成功与失败分别统计。 */
    @PostMapping("/questions/import")
    public Result<?> importQuestions(@RequestBody Map<String, Object> body) {
        Object raw = body.get("questions");
        if (!(raw instanceof List<?> list)) return Result.fail(400, "questions 必须是数组");
        int created = 0, failed = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        String now = now();
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            try {
                if (!(item instanceof Map<?, ?> m)) throw new IllegalArgumentException("第 " + (i + 1) + " 项不是对象");
                @SuppressWarnings("unchecked") Map<String, Object> q = new LinkedHashMap<>((Map<String, Object>) m);
                if (text(q.get("title")).isBlank()) throw new IllegalArgumentException("第 " + (i + 1) + " 项缺少 title");
                if (text(q.get("answer")).isBlank()) throw new IllegalArgumentException("第 " + (i + 1) + " 项缺少 answer");
                Object options = q.get("optionsJson");
                if (options != null && !(options instanceof String)) q.put("optionsJson", json.writeValueAsString(options));
                q.remove("id"); q.remove("userId");
                if (text(q.get("difficulty")).isBlank()) q.put("difficulty", "中等");
                if (text(q.get("source")).isBlank()) q.put("source", "导入");
                if (text(q.get("createdAt")).isBlank()) q.put("createdAt", now);
                service.save("questions", q);
                created++;
            } catch (Exception e) {
                failed++;
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("index", i + 1); err.put("reason", e.getMessage());
                errors.add(err);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("created", created); out.put("failed", failed); out.put("errors", errors);
        return Result.ok(out);
    }

    /** 错题复习一次：复习次数 +1，更新时间。 */
    @PostMapping("/mistakes/{id}/review")
    public Result<?> review(@PathVariable Long id) {
        Map<String, Object> m = toMap(service.get("mistakes", id));
        if (m.isEmpty()) return Result.fail(404, "错题不存在");
        assertOwned(longOf(m.get("userId")));
        int count = m.get("reviewCount") instanceof Number n ? n.intValue() : 0;
        Map<String, Object> upd = new LinkedHashMap<>();
        upd.put("id", id); upd.put("reviewCount", count + 1); upd.put("lastReviewAt", now());
        return Result.ok(service.save("mistakes", upd));
    }

    /** 标记 / 取消标记「已掌握」。 */
    @PutMapping("/mistakes/{id}/mastered")
    public Result<?> setMastered(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> m = toMap(service.get("mistakes", id));
        if (m.isEmpty()) return Result.fail(404, "错题不存在");
        assertOwned(longOf(m.get("userId")));
        boolean mastered = Boolean.TRUE.equals(body.get("mastered")) || "true".equalsIgnoreCase(text(body.get("mastered")));
        Map<String, Object> upd = new LinkedHashMap<>();
        upd.put("id", id); upd.put("mastered", mastered);
        if (mastered) upd.put("lastReviewAt", now());
        return Result.ok(service.save("mistakes", upd));
    }

    private void assertOwned(Long owner) {
        Long uid = AuthContext.userId();
        if (uid != null && !AuthContext.isAdmin() && owner != null && !uid.equals(owner)) {
            throw new SecurityException("无权操作其他账号的数据");
        }
    }

    @ExceptionHandler(SecurityException.class)
    public Result<?> handleSecurity(SecurityException e) { return Result.fail(403, e.getMessage()); }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object entity) {
        if (entity == null) return Map.of();
        return (Map<String, Object>) json.convertValue(entity, Map.class);
    }
    private static String now() { return LocalDateTime.now().format(TS); }
    private static String text(Object v) { return v == null ? "" : String.valueOf(v); }
    private static Long longOf(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o == null || String.valueOf(o).isBlank()) return null;
        try { return Long.parseLong(String.valueOf(o)); } catch (NumberFormatException e) { return null; }
    }
    private static int intOf(Object o, int fallback) {
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (RuntimeException e) { return fallback; }
    }
}
