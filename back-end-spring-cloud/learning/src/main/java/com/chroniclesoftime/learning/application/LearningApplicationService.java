package com.chroniclesoftime.learning.application;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.shared.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import com.chroniclesoftime.learning.domain.model.AnswerRecord;
import com.chroniclesoftime.learning.domain.model.MistakeRecord;
import com.chroniclesoftime.learning.domain.model.PracticeSession;
import com.chroniclesoftime.learning.domain.model.Question;
import com.chroniclesoftime.learning.domain.model.ScoreRecord;
import com.chroniclesoftime.learning.domain.repository.AnswerRecordRepository;
import com.chroniclesoftime.learning.domain.repository.MistakeRecordRepository;
import com.chroniclesoftime.learning.domain.repository.PracticeSessionRepository;
import com.chroniclesoftime.learning.domain.repository.QuestionRepository;
import com.chroniclesoftime.learning.domain.repository.ScoreRecordRepository;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/** 学习 · 应用服务：编排仓储、按账号隔离、关键词过滤与统计。 */
@Service
public class LearningApplicationService {
    private final ObjectMapper json;
    private final Map<String, Repository<?, Long>> repos = new LinkedHashMap<>();
    private final Map<String, Class<?>> types = new LinkedHashMap<>();
    private final Map<String, Boolean> owned = new HashMap<>();

    public LearningApplicationService(ObjectMapper json, QuestionRepository questionRepo, AnswerRecordRepository answerRecordRepo, MistakeRecordRepository mistakeRecordRepo, ScoreRecordRepository scoreRecordRepo, PracticeSessionRepository practiceSessionRepo) {
        this.json = json;
        repos.put("questions", questionRepo);
        types.put("questions", Question.class);
        owned.put("questions", true);
        repos.put("answers", answerRecordRepo);
        types.put("answers", AnswerRecord.class);
        owned.put("answers", true);
        repos.put("mistakes", mistakeRecordRepo);
        types.put("mistakes", MistakeRecord.class);
        owned.put("mistakes", true);
        repos.put("scores", scoreRecordRepo);
        types.put("scores", ScoreRecord.class);
        owned.put("scores", true);
        repos.put("sessions", practiceSessionRepo);
        types.put("sessions", PracticeSession.class);
        owned.put("sessions", true);
    }

    public Set<String> resources() { return repos.keySet(); }
    public Repository<?, Long> repo(String resource) { return require(resource); }
    public Class<?> type(String resource) { return types.get(requireKey(resource)); }

    /** 默认只返回当前账号数据；管理员显式 all=true 时可跨账号核查。 */
    public List<Object> list(String resource, boolean all, String keyword) {
        Repository raw = require(resource);
        Long uid = AuthContext.userId();
        boolean scoped = !all && Boolean.TRUE.equals(owned.get(resource)) && uid != null;
        List<?> rows = scoped ? raw.findBy("user_id", uid) : raw.findAll();
        List<Object> out = new ArrayList<>(rows);
        out.sort((a, b) -> Long.compare(idOf(b), idOf(a)));
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            out = out.stream().filter(row -> {
                Map<?, ?> m = json.convertValue(row, Map.class);
                return m.values().stream().anyMatch(v -> v != null && String.valueOf(v).toLowerCase().contains(kw));
            }).collect(Collectors.toList());
        }
        return out;
    }

    public Object get(String resource, Long id) { return require(resource).findById(id); }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object save(String resource, Map<String, Object> body) {
        Repository raw = require(resource);
        Object entity = json.convertValue(body, types.get(requireKey(resource)));
        if (Boolean.TRUE.equals(owned.get(resource)) && body.get("id") == null && AuthContext.userId() != null) {
            try {
                Method setter = entity.getClass().getMethod("setUserId", Long.class);
                setter.invoke(entity, AuthContext.userId());
            } catch (ReflectiveOperationException ignored) { /* 实体无 userId 列 */ }
        }
        return raw.save(entity);
    }

    public boolean delete(String resource, Long id) { return require(resource).deleteById(id); }

    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("module", "learning");
        repos.forEach((k, v) -> m.put(k, v.count()));
        return m;
    }

    /** 供领域特性接口复用的原始数据视图。 */
    public List<Map<String, Object>> rows(String resource) {
        return list(resource, true, null).stream()
                .map(r -> (Map<String, Object>) json.convertValue(r, Map.class))
                .collect(Collectors.toList());
    }

    private Repository require(String resource) {
        Repository r = repos.get(resource);
        if (r == null) throw new IllegalArgumentException("未知资源: " + resource);
        return r;
    }
    private String requireKey(String resource) {
        if (!repos.containsKey(resource)) throw new IllegalArgumentException("未知资源: " + resource);
        return resource;
    }
    private long idOf(Object entity) {
        try {
            Object v = entity.getClass().getMethod("getId").invoke(entity);
            return v instanceof Number ? ((Number) v).longValue() : 0L;
        } catch (ReflectiveOperationException e) { return 0L; }
    }
}
