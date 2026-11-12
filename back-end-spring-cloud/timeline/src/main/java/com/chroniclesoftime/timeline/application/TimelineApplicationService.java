package com.chroniclesoftime.timeline.application;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.shared.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import com.chroniclesoftime.timeline.domain.model.AdvanceRecord;
import com.chroniclesoftime.timeline.domain.model.GrowthLink;
import com.chroniclesoftime.timeline.domain.model.LifeRecord;
import com.chroniclesoftime.timeline.domain.model.Milestone;
import com.chroniclesoftime.timeline.domain.model.WorkRecord;
import com.chroniclesoftime.timeline.domain.repository.AdvanceRecordRepository;
import com.chroniclesoftime.timeline.domain.repository.GrowthLinkRepository;
import com.chroniclesoftime.timeline.domain.repository.LifeRecordRepository;
import com.chroniclesoftime.timeline.domain.repository.MilestoneRepository;
import com.chroniclesoftime.timeline.domain.repository.WorkRecordRepository;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** 拾光时间线 · 应用服务：编排仓储、按账号隔离、关键词过滤、里程碑联动与统计。 */
@Service
public class TimelineApplicationService {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper json;
    private final MilestoneRepository milestoneRepo;
    private final Map<String, Repository<?, Long>> repos = new LinkedHashMap<>();
    private final Map<String, Class<?>> types = new LinkedHashMap<>();
    private final Map<String, Boolean> owned = new HashMap<>();

    public TimelineApplicationService(ObjectMapper json, LifeRecordRepository lifeRecordRepo, MilestoneRepository milestoneRepo, GrowthLinkRepository growthLinkRepo, WorkRecordRepository workRecordRepo, AdvanceRecordRepository advanceRecordRepo) {
        this.json = json;
        this.milestoneRepo = milestoneRepo;
        repos.put("records", lifeRecordRepo);
        types.put("records", LifeRecord.class);
        owned.put("records", true);
        repos.put("milestones", milestoneRepo);
        types.put("milestones", Milestone.class);
        owned.put("milestones", true);
        repos.put("links", growthLinkRepo);
        types.put("links", GrowthLink.class);
        owned.put("links", true);
        repos.put("work-records", workRecordRepo);
        types.put("work-records", WorkRecord.class);
        owned.put("work-records", true);
        repos.put("advance-records", advanceRecordRepo);
        types.put("advance-records", AdvanceRecord.class);
        owned.put("advance-records", true);
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
        boolean isNew = body.get("id") == null;
        if (Boolean.TRUE.equals(owned.get(resource)) && isNew && AuthContext.userId() != null) {
            try {
                Method setter = entity.getClass().getMethod("setUserId", Long.class);
                setter.invoke(entity, AuthContext.userId());
            } catch (ReflectiveOperationException ignored) { /* 实体无 userId 列 */ }
        }
        if ("milestones".equals(resource) && entity instanceof Milestone ms && ms.getOrigin() == null) {
            ms.setOrigin("MANUAL");
        }
        Object saved = raw.save(entity);
        // D1 双通道联动：记录勾选「标为里程碑」时自动创建 AUTO 里程碑；取消勾选时回收 AUTO 里程碑。
        if ("records".equals(resource)) syncAutoMilestone(saved);
        return saved;
    }

    public boolean delete(String resource, Long id) {
        // 删除成长记录时，同步回收其自动生成的里程碑，避免时间线上留下孤儿节点。
        if ("records".equals(resource)) removeAutoMilestones(id, true);
        return require(resource).deleteById(id);
    }

    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("module", "timeline");
        repos.forEach((k, v) -> m.put(k, v.count()));
        return m;
    }

    /** 供跨域时间线聚合复用的全量原始数据视图（由调用方自行做账号隔离）。 */
    public List<Map<String, Object>> rows(String resource) {
        return list(resource, true, null).stream()
                .map(r -> (Map<String, Object>) json.convertValue(r, Map.class))
                .collect(Collectors.toList());
    }

    /** 供领域特性接口复用的账号隔离数据视图；未登录且非管理员核查时返回空。 */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> rowsScoped(String resource, boolean all) {
        boolean crossAccount = all && AuthContext.isAdmin();
        if (!crossAccount && AuthContext.userId() == null) return List.of();
        return list(resource, crossAccount, null).stream()
                .map(r -> (Map<String, Object>) json.convertValue(r, Map.class))
                .collect(Collectors.toList());
    }

    /** 里程碑详情：带来源标记、关联记录的标题与日期，供里程碑页区分 AUTO / MANUAL。 */
    public List<Map<String, Object>> milestoneDetails(boolean all) {
        Map<Long, Map<String, Object>> recordIndex = new HashMap<>();
        for (Map<String, Object> r : rowsScoped("records", all)) {
            if (r.get("id") instanceof Number n) recordIndex.put(n.longValue(), r);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> m : rowsScoped("milestones", all)) {
            Map<String, Object> one = new LinkedHashMap<>(m);
            one.put("origin", one.get("origin") == null ? "MANUAL" : String.valueOf(one.get("origin")));
            if (one.get("linkedRecordId") instanceof Number n && recordIndex.containsKey(n.longValue())) {
                Map<String, Object> r = recordIndex.get(n.longValue());
                one.put("linkedRecordTitle", r.get("title"));
                one.put("linkedRecordDate", r.get("recordDate"));
            }
            out.add(one);
        }
        out.sort((a, b) -> String.valueOf(b.getOrDefault("occurredAt", ""))
                .compareTo(String.valueOf(a.getOrDefault("occurredAt", ""))));
        return out;
    }

    /** 手动把里程碑关联到一条成长记录；关联后交由用户管理，标记 MANUAL。 */
    public Map<String, Object> linkMilestone(Long id, Long recordId) {
        Milestone ms = milestoneRepo.findById(id);
        if (ms == null) throw new IllegalArgumentException("里程碑不存在: " + id);
        assertOwned(ms.getUserId());
        if (recordId == null) throw new IllegalArgumentException("请选择要关联的成长记录");
        Object found = repos.get("records").findById(recordId);
        if (!(found instanceof LifeRecord record)) throw new IllegalArgumentException("成长记录不存在: " + recordId);
        assertOwned(record.getUserId());
        ms.setLinkedRecordId(recordId);
        ms.setOrigin("MANUAL");
        milestoneRepo.save(ms);
        return (Map<String, Object>) json.convertValue(ms, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> unlinkMilestone(Long id) {
        Milestone ms = milestoneRepo.findById(id);
        if (ms == null) throw new IllegalArgumentException("里程碑不存在: " + id);
        assertOwned(ms.getUserId());
        // updateById 会忽略 null 字段，必须走 UpdateWrapper 才能真正清空关联。
        milestoneRepo.clearLinkedRecord(id);
        return (Map<String, Object>) json.convertValue(milestoneRepo.findById(id), Map.class);
    }

    /** 记录 → 里程碑自动联动。已有任意关联（含手动关联）时不重复生成。 */
    private void syncAutoMilestone(Object savedRecord) {
        @SuppressWarnings("unchecked")
        Map<String, Object> rec = (Map<String, Object>) json.convertValue(savedRecord, Map.class);
        if (!(rec.get("id") instanceof Number num)) return;
        long recordId = num.longValue();
        List<Milestone> linked = milestoneRepo.findBy("linked_record_id", recordId);
        boolean flagged = Boolean.TRUE.equals(rec.get("isMilestone"));
        if (!flagged) {
            removeAutoMilestones(recordId, false);
            return;
        }
        if (!linked.isEmpty()) return;
        Milestone ms = new Milestone();
        Long owner = rec.get("userId") instanceof Number n ? n.longValue() : AuthContext.userId();
        ms.setUserId(owner);
        ms.setTitle(String.valueOf(rec.getOrDefault("title", "成长里程碑")));
        ms.setOccurredAt(String.valueOf(rec.getOrDefault("recordDate", "")));
        ms.setDescription(String.valueOf(rec.getOrDefault("content", "")));
        ms.setIcon("star");
        ms.setColor("mint");
        ms.setLinkedRecordId(recordId);
        ms.setOrigin("AUTO");
        ms.setCreatedAt(LocalDateTime.now().format(TS));
        milestoneRepo.save(ms);
    }

    private void removeAutoMilestones(Long recordId, boolean deleteAllLinked) {
        // 一次取出所有关联：自动生成的删除；手动关联的（删除记录时）解除引用避免悬空。
        for (Milestone ms : milestoneRepo.findBy("linked_record_id", recordId)) {
            if ("AUTO".equals(ms.getOrigin())) milestoneRepo.deleteById(ms.getId());
            else if (deleteAllLinked) milestoneRepo.clearLinkedRecord(ms.getId());
        }
    }

    private void assertOwned(Long owner) {
        Long uid = AuthContext.userId();
        if (uid != null && !AuthContext.isAdmin() && owner != null && !uid.equals(owner)) {
            throw new SecurityException("无权操作其他账号的数据");
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
}
