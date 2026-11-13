package com.chroniclesoftime.planning.application;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.shared.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import com.chroniclesoftime.planning.domain.model.AdmissionSimulation;
import com.chroniclesoftime.planning.domain.model.ChecklistItem;
import com.chroniclesoftime.planning.domain.model.CourseGuidance;
import com.chroniclesoftime.planning.domain.model.CourseSelection;
import com.chroniclesoftime.planning.domain.model.GradingScale;
import com.chroniclesoftime.planning.domain.model.GraduationGap;
import com.chroniclesoftime.planning.domain.model.MajorRequirement;
import com.chroniclesoftime.planning.domain.model.QualityRecord;
import com.chroniclesoftime.planning.domain.model.ReviewPlan;
import com.chroniclesoftime.planning.domain.model.SelectionHistory;
import com.chroniclesoftime.planning.domain.model.SelectionIntention;
import com.chroniclesoftime.planning.domain.model.SubjectCombination;
import com.chroniclesoftime.planning.domain.model.SubjectOption;
import com.chroniclesoftime.planning.domain.model.TargetPath;
import com.chroniclesoftime.planning.domain.model.TermCourse;
import com.chroniclesoftime.planning.domain.model.VolunteerDetail;
import com.chroniclesoftime.planning.domain.model.VolunteerPlan;
import com.chroniclesoftime.planning.domain.repository.AdmissionSimulationRepository;
import com.chroniclesoftime.planning.domain.repository.ChecklistItemRepository;
import com.chroniclesoftime.planning.domain.repository.CourseGuidanceRepository;
import com.chroniclesoftime.planning.domain.repository.CourseSelectionRepository;
import com.chroniclesoftime.planning.domain.repository.GradingScaleRepository;
import com.chroniclesoftime.planning.domain.repository.GraduationGapRepository;
import com.chroniclesoftime.planning.domain.repository.MajorRequirementRepository;
import com.chroniclesoftime.planning.domain.repository.QualityRecordRepository;
import com.chroniclesoftime.planning.domain.repository.ReviewPlanRepository;
import com.chroniclesoftime.planning.domain.repository.SelectionHistoryRepository;
import com.chroniclesoftime.planning.domain.repository.SelectionIntentionRepository;
import com.chroniclesoftime.planning.domain.repository.SubjectCombinationRepository;
import com.chroniclesoftime.planning.domain.repository.SubjectOptionRepository;
import com.chroniclesoftime.planning.domain.repository.TargetPathRepository;
import com.chroniclesoftime.planning.domain.repository.TermCourseRepository;
import com.chroniclesoftime.planning.domain.repository.VolunteerDetailRepository;
import com.chroniclesoftime.planning.domain.repository.VolunteerPlanRepository;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/** 规划 · 应用服务：编排仓储、按账号隔离、关键词过滤与统计。 */
@Service
public class PlanningApplicationService {
    private final ObjectMapper json;
    private final Map<String, Repository<?, Long>> repos = new LinkedHashMap<>();
    private final Map<String, Class<?>> types = new LinkedHashMap<>();
    private final Map<String, Boolean> owned = new HashMap<>();

    public PlanningApplicationService(ObjectMapper json, SubjectOptionRepository subjectOptionRepo, SubjectCombinationRepository subjectCombinationRepo, CourseSelectionRepository courseSelectionRepo, SelectionIntentionRepository selectionIntentionRepo, CourseGuidanceRepository courseGuidanceRepo, SelectionHistoryRepository selectionHistoryRepo, GradingScaleRepository gradingScaleRepo, MajorRequirementRepository majorRequirementRepo, VolunteerPlanRepository volunteerPlanRepo, VolunteerDetailRepository volunteerDetailRepo, AdmissionSimulationRepository admissionSimulationRepo, TermCourseRepository termCourseRepo, GraduationGapRepository graduationGapRepo, ReviewPlanRepository reviewPlanRepo, TargetPathRepository targetPathRepo, QualityRecordRepository qualityRecordRepo, ChecklistItemRepository checklistItemRepo) {
        this.json = json;
        repos.put("subject-options", subjectOptionRepo);
        types.put("subject-options", SubjectOption.class);
        owned.put("subject-options", false);
        repos.put("subject-combinations", subjectCombinationRepo);
        types.put("subject-combinations", SubjectCombination.class);
        owned.put("subject-combinations", true);
        repos.put("selections", courseSelectionRepo);
        types.put("selections", CourseSelection.class);
        owned.put("selections", true);
        repos.put("intentions", selectionIntentionRepo);
        types.put("intentions", SelectionIntention.class);
        owned.put("intentions", true);
        repos.put("guidances", courseGuidanceRepo);
        types.put("guidances", CourseGuidance.class);
        owned.put("guidances", true);
        repos.put("selection-histories", selectionHistoryRepo);
        types.put("selection-histories", SelectionHistory.class);
        owned.put("selection-histories", true);
        repos.put("grading-scales", gradingScaleRepo);
        types.put("grading-scales", GradingScale.class);
        owned.put("grading-scales", false);
        repos.put("major-requirements", majorRequirementRepo);
        types.put("major-requirements", MajorRequirement.class);
        owned.put("major-requirements", false);
        repos.put("volunteer-plans", volunteerPlanRepo);
        types.put("volunteer-plans", VolunteerPlan.class);
        owned.put("volunteer-plans", true);
        repos.put("volunteer-details", volunteerDetailRepo);
        types.put("volunteer-details", VolunteerDetail.class);
        owned.put("volunteer-details", true);
        repos.put("simulations", admissionSimulationRepo);
        types.put("simulations", AdmissionSimulation.class);
        owned.put("simulations", true);
        repos.put("term-courses", termCourseRepo);
        types.put("term-courses", TermCourse.class);
        owned.put("term-courses", true);
        repos.put("graduation-gaps", graduationGapRepo);
        types.put("graduation-gaps", GraduationGap.class);
        owned.put("graduation-gaps", true);
        repos.put("review-plans", reviewPlanRepo);
        types.put("review-plans", ReviewPlan.class);
        owned.put("review-plans", true);
        repos.put("target-paths", targetPathRepo);
        types.put("target-paths", TargetPath.class);
        owned.put("target-paths", true);
        repos.put("quality-records", qualityRecordRepo);
        types.put("quality-records", QualityRecord.class);
        owned.put("quality-records", true);
        repos.put("checklist-items", checklistItemRepo);
        types.put("checklist-items", ChecklistItem.class);
        owned.put("checklist-items", true);
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
        m.put("module", "planning");
        repos.forEach((k, v) -> m.put(k, v.count()));
        return m;
    }

    /** 供领域特性接口复用的原始数据视图（全量，仅供内部/管理员统计使用）。 */
    public List<Map<String, Object>> rows(String resource) {
        return list(resource, true, null).stream()
                .map(r -> (Map<String, Object>) json.convertValue(r, Map.class))
                .collect(Collectors.toList());
    }

    /**
     * 特性接口默认使用：按当前账号隔离；管理员显式 all=true 时可跨账号核查。
     * 字典类资源（owned=false，如 subject-options / major-requirements）本来就是全局共享。
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> rowsScoped(String resource, boolean all) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object r : list(resource, all, null)) {
            out.add((Map<String, Object>) json.convertValue(r, Map.class));
        }
        return out;
    }

    /** 按单列精确查找（工作流内部使用，如按 id / planId 定位）。显式循环避免 raw Repository 泛型推断问题。 */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findBy(String resource, String column, Object value) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object r : require(resource).findBy(column, value)) {
            out.add((Map<String, Object>) json.convertValue(r, Map.class));
        }
        return out;
    }

    /** 按 id 读取并转成 Map；找不到返回 null。 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> row(String resource, Long id) {
        Object r = get(resource, id);
        return r == null ? null : (Map<String, Object>) json.convertValue(r, Map.class);
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
