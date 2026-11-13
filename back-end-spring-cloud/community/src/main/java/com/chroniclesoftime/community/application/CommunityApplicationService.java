package com.chroniclesoftime.community.application;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.shared.BizException;
import com.chroniclesoftime.shared.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import com.chroniclesoftime.community.domain.model.Content;
import com.chroniclesoftime.community.domain.model.ContentComment;
import com.chroniclesoftime.community.domain.model.FavoriteRecord;
import com.chroniclesoftime.community.domain.model.LikeRecord;
import com.chroniclesoftime.community.domain.repository.ContentCommentRepository;
import com.chroniclesoftime.community.domain.repository.ContentRepository;
import com.chroniclesoftime.community.domain.repository.FavoriteRecordRepository;
import com.chroniclesoftime.community.domain.repository.LikeRecordRepository;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/** 社区 · 应用服务：编排仓储、按账号隔离、公开已发布视图与关键词过滤。 */
@Service
public class CommunityApplicationService {
    private final ObjectMapper json;
    private final Map<String, Repository<?, Long>> repos = new LinkedHashMap<>();
    private final Map<String, Class<?>> types = new LinkedHashMap<>();
    private final Map<String, Boolean> owned = new HashMap<>();

    public CommunityApplicationService(ObjectMapper json, ContentRepository contentRepo, ContentCommentRepository contentCommentRepo, LikeRecordRepository likeRecordRepo, FavoriteRecordRepository favoriteRecordRepo) {
        this.json = json;
        repos.put("contents", contentRepo);
        types.put("contents", Content.class);
        owned.put("contents", true);
        repos.put("comments", contentCommentRepo);
        types.put("comments", ContentComment.class);
        owned.put("comments", true);
        repos.put("likes", likeRecordRepo);
        types.put("likes", LikeRecord.class);
        owned.put("likes", true);
        repos.put("favorites", favoriteRecordRepo);
        types.put("favorites", FavoriteRecord.class);
        owned.put("favorites", true);
    }

    public Set<String> resources() { return repos.keySet(); }
    public Repository<?, Long> repo(String resource) { return require(resource); }
    public Class<?> type(String resource) { return types.get(requireKey(resource)); }

    /** 默认只返回当前账号数据；跨账号 all=true 仅管理员可用，普通用户会被强制隔离。 */
    public List<Object> list(String resource, boolean all, String keyword) {
        Repository raw = require(resource);
        Long uid = AuthContext.userId();
        boolean crossAccount = all && AuthContext.isAdmin() && uid != null;
        boolean scoped = !crossAccount && Boolean.TRUE.equals(owned.get(resource)) && uid != null;
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

    /** 通用写接口：带 id 时校验归属，防止改 / 删其他账号的内容。 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object save(String resource, Map<String, Object> body) {
        assertWritable(resource, body.get("id"));
        return doSave(resource, body);
    }

    /** 内部计数 / 状态维护专用：调用方已自行完成权限校验，跳过归属检查。 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object saveInternal(String resource, Map<String, Object> body) {
        return doSave(resource, body);
    }

    public boolean delete(String resource, Long id) {
        assertWritable(resource, id);
        return require(resource).deleteById(id);
    }

    public boolean deleteInternal(String resource, Long id) { return require(resource).deleteById(id); }

    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("module", "community");
        repos.forEach((k, v) -> m.put(k, v.count()));
        return m;
    }

    /** 全量原始视图：仅供管理员核查与服务端内部聚合，绝不可直接暴露给公开端点。 */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> rows(String resource) {
        return list(resource, true, null).stream()
                .map(r -> (Map<String, Object>) json.convertValue(r, Map.class))
                .collect(Collectors.toList());
    }

    /** 按当前账号隔离的原始视图。 */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> rowsScoped(String resource) {
        return list(resource, false, null).stream()
                .map(r -> (Map<String, Object>) json.convertValue(r, Map.class))
                .collect(Collectors.toList());
    }

    /** 公开只读视图：只返回 status = PUBLISHED 的文章，按 id 倒序。 */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> published() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : require("contents").findAll()) {
            Map<String, Object> m = (Map<String, Object>) json.convertValue(o, Map.class);
            if ("PUBLISHED".equals(String.valueOf(m.get("status")))) out.add(m);
        }
        out.sort((a, b) -> Long.compare(longOf(b.get("id")) == null ? 0 : longOf(b.get("id")),
                longOf(a.get("id")) == null ? 0 : longOf(a.get("id"))));
        return out;
    }

    /** 按单列精确查找（工作流内部使用）。raw Repository 返回裸 List，用显式循环避免泛型推断问题。 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Map<String, Object>> findBy(String resource, String column, Object value) {
        List<Map<String, Object>> out = new ArrayList<>();
        Repository raw = require(resource);
        for (Object o : raw.findBy(column, value)) out.add((Map<String, Object>) json.convertValue(o, Map.class));
        return out;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> row(String resource, Long id) {
        Object o = require(resource).findById(id);
        return o == null ? null : (Map<String, Object>) json.convertValue(o, Map.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object doSave(String resource, Map<String, Object> body) {
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

    private void assertWritable(String resource, Object idValue) {
        Long id = longOf(idValue);
        if (id == null || !Boolean.TRUE.equals(owned.get(resource)) || AuthContext.isAdmin()) return;
        Object existing = require(resource).findById(id);
        if (existing == null) return;
        Map<?, ?> m = json.convertValue(existing, Map.class);
        Object owner = m.get("userId");
        Long uid = AuthContext.userId();
        Long ownerId = longOf(owner);
        if (uid == null || ownerId == null || !uid.equals(ownerId)) {
            throw new BizException(403, "无权操作其他账号的数据");
        }
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
    private static Long longOf(Object o) { return o instanceof Number n ? n.longValue() : null; }
}
