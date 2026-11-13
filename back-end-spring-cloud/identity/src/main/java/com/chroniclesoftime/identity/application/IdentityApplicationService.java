package com.chroniclesoftime.identity.application;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.shared.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import com.chroniclesoftime.identity.domain.model.Account;
import com.chroniclesoftime.identity.domain.model.AuditLog;
import com.chroniclesoftime.identity.domain.model.ResumeSection;
import com.chroniclesoftime.identity.domain.model.UserProfile;
import com.chroniclesoftime.identity.domain.repository.AccountRepository;
import com.chroniclesoftime.identity.domain.repository.AuditLogRepository;
import com.chroniclesoftime.identity.domain.repository.ResumeSectionRepository;
import com.chroniclesoftime.identity.domain.repository.UserProfileRepository;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/** 身份与访问 · 应用服务：编排仓储、按账号隔离、关键词过滤与统计。 */
@Service
public class IdentityApplicationService {
    private final ObjectMapper json;
    private final Map<String, Repository<?, Long>> repos = new LinkedHashMap<>();
    private final Map<String, Class<?>> types = new LinkedHashMap<>();
    private final Map<String, Boolean> owned = new HashMap<>();

    public IdentityApplicationService(ObjectMapper json, AccountRepository accountRepo, AuditLogRepository auditLogRepo, UserProfileRepository userProfileRepo, ResumeSectionRepository resumeSectionRepo) {
        this.json = json;
        repos.put("accounts", accountRepo);
        types.put("accounts", Account.class);
        owned.put("accounts", false);
        repos.put("audit-logs", auditLogRepo);
        types.put("audit-logs", AuditLog.class);
        owned.put("audit-logs", false);
        repos.put("profiles", userProfileRepo);
        types.put("profiles", UserProfile.class);
        owned.put("profiles", true);
        repos.put("resume-sections", resumeSectionRepo);
        types.put("resume-sections", ResumeSection.class);
        owned.put("resume-sections", true);
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
        m.put("module", "identity");
        repos.forEach((k, v) -> m.put(k, v.count()));
        return m;
    }

    /** 供领域特性接口复用的原始数据视图。 */
    public List<Map<String, Object>> rows(String resource) {
        return list(resource, true, null).stream()
                .map(r -> (Map<String, Object>) json.convertValue(r, Map.class))
                .collect(Collectors.toList());
    }

    /** 按当前账号隔离的原始视图：简历等私有数据一律走这里。 */
    public List<Map<String, Object>> rowsScoped(String resource) {
        return list(resource, false, null).stream()
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
