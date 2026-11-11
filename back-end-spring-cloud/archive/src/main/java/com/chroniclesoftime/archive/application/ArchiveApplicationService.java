package com.chroniclesoftime.archive.application;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.shared.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import com.chroniclesoftime.archive.domain.model.Certificate;
import com.chroniclesoftime.archive.domain.model.CourseTreeNode;
import com.chroniclesoftime.archive.domain.model.ElectivePost;
import com.chroniclesoftime.archive.domain.model.Paper;
import com.chroniclesoftime.archive.domain.model.PaperSuggestion;
import com.chroniclesoftime.archive.domain.model.ThesisBoardItem;
import com.chroniclesoftime.archive.domain.repository.CertificateRepository;
import com.chroniclesoftime.archive.domain.repository.CourseTreeNodeRepository;
import com.chroniclesoftime.archive.domain.repository.ElectivePostRepository;
import com.chroniclesoftime.archive.domain.repository.PaperRepository;
import com.chroniclesoftime.archive.domain.repository.PaperSuggestionRepository;
import com.chroniclesoftime.archive.domain.repository.ThesisBoardItemRepository;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/** 档案 · 应用服务：编排仓储、按账号隔离、关键词过滤与统计。 */
@Service
public class ArchiveApplicationService {
    private final ObjectMapper json;
    private final Map<String, Repository<?, Long>> repos = new LinkedHashMap<>();
    private final Map<String, Class<?>> types = new LinkedHashMap<>();
    private final Map<String, Boolean> owned = new HashMap<>();

    public ArchiveApplicationService(ObjectMapper json, CertificateRepository certificateRepo, PaperRepository paperRepo, PaperSuggestionRepository paperSuggestionRepo, CourseTreeNodeRepository courseTreeNodeRepo, ThesisBoardItemRepository thesisBoardItemRepo, ElectivePostRepository electivePostRepo) {
        this.json = json;
        repos.put("certificates", certificateRepo);
        types.put("certificates", Certificate.class);
        owned.put("certificates", true);
        repos.put("papers", paperRepo);
        types.put("papers", Paper.class);
        owned.put("papers", true);
        repos.put("paper-suggestions", paperSuggestionRepo);
        types.put("paper-suggestions", PaperSuggestion.class);
        owned.put("paper-suggestions", true);
        repos.put("course-tree", courseTreeNodeRepo);
        types.put("course-tree", CourseTreeNode.class);
        owned.put("course-tree", true);
        repos.put("thesis-board", thesisBoardItemRepo);
        types.put("thesis-board", ThesisBoardItem.class);
        owned.put("thesis-board", true);
        repos.put("elective-posts", electivePostRepo);
        types.put("elective-posts", ElectivePost.class);
        owned.put("elective-posts", true);
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
        m.put("module", "archive");
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
