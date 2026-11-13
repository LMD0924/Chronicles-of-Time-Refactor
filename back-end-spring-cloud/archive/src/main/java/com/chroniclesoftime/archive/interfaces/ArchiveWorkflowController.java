package com.chroniclesoftime.archive.interfaces;

import com.chroniclesoftime.archive.application.ArchiveApplicationService;
import com.chroniclesoftime.shared.Result;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 档案 · 业务工作流：论文看板阶段流转、课程树层级维护、选课参考聚合与附件绑定。 */
@RestController
@RequestMapping("/api/v1/archive/workflow")
public class ArchiveWorkflowController {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> STAGES = List.of("选题", "开题", "初稿", "修改", "定稿", "答辩");
    private static final List<String> STATUSES = List.of("待办", "进行中", "已完成");
    private static final List<String> NODE_TYPES = List.of("分类", "课程", "知识点");

    private final ArchiveApplicationService service;
    public ArchiveWorkflowController(ArchiveApplicationService service) { this.service = service; }

    @GetMapping("/overview")
    public Result<?> overview() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("certificates", service.rowsScoped("certificates").size());
        m.put("papers", service.rowsScoped("papers").size());
        m.put("courseNodes", service.rowsScoped("course-tree").size());
        m.put("thesisItems", service.rowsScoped("thesis-board").size());
        m.put("electivePosts", service.rowsScoped("elective-posts").size());
        m.put("stages", STAGES);
        return Result.ok(m);
    }

    // ============================================================ 论文看板

    @GetMapping("/thesis/board")
    public Result<?> thesisBoard() {
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (String s : STAGES) grouped.put(s, new ArrayList<>());
        List<Map<String, Object>> rows = service.rowsScoped("thesis-board");
        int done = 0, overdue = 0;
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        for (Map<String, Object> r : rows) {
            String stage = text(r.get("stage"));
            grouped.computeIfAbsent(stage.isBlank() ? "选题" : stage, k -> new ArrayList<>()).add(r);
            if ("已完成".equals(text(r.get("status")))) done++;
            String deadline = text(r.get("deadline"));
            if (!deadline.isBlank() && deadline.compareTo(today) < 0 && !"已完成".equals(text(r.get("status")))) overdue++;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("groups", grouped);
        out.put("stages", STAGES);
        out.put("total", rows.size());
        out.put("done", done);
        out.put("overdue", overdue);
        out.put("completionRate", rows.isEmpty() ? 0 : Math.round(done * 100.0 / rows.size()));
        return Result.ok(out);
    }

    /** 阶段推进：可指定 stage，或 next=true 顺推一阶段；完成时自动置为已完成。 */
    @PutMapping("/thesis/{id}/stage")
    public Result<?> advanceStage(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> row = service.row("thesis-board", id);
        if (row == null) return Result.fail(404, "看板任务不存在");
        Map<String, Object> args = body == null ? Map.of() : body;
        String target;
        if (Boolean.TRUE.equals(args.get("next"))) {
            int idx = STAGES.indexOf(text(row.get("stage")));
            if (idx < 0) idx = 0;
            target = STAGES.get(Math.min(idx + 1, STAGES.size() - 1));
        } else {
            target = text(args.get("stage"));
            if (!STAGES.contains(target)) return Result.fail(400, "阶段只能是 " + String.join(" / ", STAGES));
        }
        Map<String, Object> patch = new LinkedHashMap<>(row);
        patch.put("stage", target);
        if (Boolean.TRUE.equals(args.get("done"))) patch.put("status", "已完成");
        service.saveInternal("thesis-board", patch);
        return Result.ok(Map.of("id", id, "stage", target, "item", patch));
    }

    @PutMapping("/thesis/{id}/status")
    public Result<?> setStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> row = service.row("thesis-board", id);
        if (row == null) return Result.fail(404, "看板任务不存在");
        String status = text(body.get("status"));
        if (!STATUSES.contains(status)) return Result.fail(400, "状态只能是 " + String.join(" / ", STATUSES));
        Map<String, Object> patch = new LinkedHashMap<>(row);
        patch.put("status", status);
        service.saveInternal("thesis-board", patch);
        return Result.ok(Map.of("id", id, "status", status));
    }

    // ============================================================ 课程树

    @PostMapping("/course-tree")
    public Result<?> addNode(@RequestBody Map<String, Object> body) {
        String name = text(body.get("name")).trim();
        if (name.isBlank()) return Result.fail(400, "节点名称不能为空");
        String type = text(body.get("nodeType"));
        Long parentId = longOf(body.get("parentId"));
        if (parentId != null && parentId != 0) {
            Map<String, Object> parent = service.row("course-tree", parentId);
            if (parent == null || !inScope(parentId)) return Result.fail(400, "父节点不存在");
        }
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("name", name);
        node.put("parentId", parentId == null ? 0L : parentId);
        node.put("nodeType", NODE_TYPES.contains(type) ? type : "分类");
        node.put("credit", body.get("credit") == null ? 0 : body.get("credit"));
        node.put("sortNo", body.get("sortNo") == null ? 0 : body.get("sortNo"));
        node.put("description", text(body.get("description")));
        return Result.ok(service.save("course-tree", node));
    }

    @PutMapping("/course-tree/{id}")
    public Result<?> updateNode(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> row = service.row("course-tree", id);
        if (row == null || !inScope(id)) return Result.fail(404, "节点不存在");
        Map<String, Object> patch = new LinkedHashMap<>(row);
        for (String k : List.of("name", "nodeType", "credit", "sortNo", "description")) {
            if (body.containsKey(k)) patch.put(k, body.get(k));
        }
        Long parentId = longOf(body.get("parentId"));
        if (parentId != null) {
            if (parentId.equals(id)) return Result.fail(400, "父节点不能是自己");
            if (parentId != 0 && service.row("course-tree", parentId) == null) return Result.fail(400, "父节点不存在");
            patch.put("parentId", parentId);
        }
        if (text(patch.get("name")).isBlank()) return Result.fail(400, "节点名称不能为空");
        return Result.ok(service.saveInternal("course-tree", patch));
    }

    /** 删除节点：子节点提升为根节点，避免误删整棵子树。 */
    @DeleteMapping("/course-tree/{id}")
    public Result<?> deleteNode(@PathVariable Long id) {
        Map<String, Object> row = service.row("course-tree", id);
        if (row == null || !inScope(id)) return Result.fail(404, "节点不存在");
        int promoted = 0;
        for (Map<String, Object> child : service.findBy("course-tree", "parent_id", id)) {
            Map<String, Object> patch = new LinkedHashMap<>(child);
            patch.put("parentId", 0L);
            service.saveInternal("course-tree", patch);
            promoted++;
        }
        service.deleteInternal("course-tree", id);
        return Result.ok(Map.of("id", id, "deleted", true, "promotedChildren", promoted));
    }

    // ============================================================ 选课参考 / 附件

    @GetMapping("/elective/summary")
    public Result<?> electiveSummary() {
        Map<String, List<Double>> byCourse = new TreeMap<>();
        for (Map<String, Object> p : service.rowsScoped("elective-posts")) {
            String course = text(p.get("courseName"));
            if (course.isBlank()) continue;
            double rating = p.get("rating") instanceof Number n ? n.doubleValue() : 0;
            byCourse.computeIfAbsent(course, k -> new ArrayList<>()).add(rating);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        byCourse.forEach((course, ratings) -> {
            double avg = ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("courseName", course);
            m.put("reviews", ratings.size());
            m.put("avgRating", Math.round(avg * 10) / 10.0);
            m.put("maxRating", ratings.stream().mapToDouble(Double::doubleValue).max().orElse(0));
            out.add(m);
        });
        out.sort((a, b) -> Double.compare((Double) b.get("avgRating"), (Double) a.get("avgRating")));
        return Result.ok(Map.of("items", out, "courses", out.size()));
    }

    /** 把已上传的文件 URL 绑定到证书 / 论文，只接受本服务 /files/ 下的静态资源。 */
    @PutMapping("/attachments/bind")
    public Result<?> bindAttachment(@RequestBody Map<String, Object> body) {
        String resource = text(body.get("resource"));
        if (!"certificates".equals(resource) && !"papers".equals(resource)) {
            return Result.fail(400, "resource 只能是 certificates / papers");
        }
        Long id = longOf(body.get("id"));
        String url = text(body.get("url")).trim();
        if (id == null || !url.startsWith("/files/") || url.contains("..")) {
            return Result.fail(400, "附件地址无效，请先通过上传接口获取 URL");
        }
        Map<String, Object> row = service.row(resource, id);
        if (row == null || !inScope(resource, id)) return Result.fail(404, "记录不存在");
        Map<String, Object> patch = new LinkedHashMap<>(row);
        patch.put("fileUrl", url);
        if ("papers".equals(resource)) patch.put("updatedAt", now());
        service.saveInternal(resource, patch);
        return Result.ok(Map.of("id", id, "resource", resource, "fileUrl", url));
    }

    private boolean inScope(Long id) { return inScope("course-tree", id); }
    private boolean inScope(String resource, Long id) {
        return service.rowsScoped(resource).stream().anyMatch(r -> id.equals(longOf(r.get("id"))));
    }
    private static String text(Object v) { return v == null ? "" : String.valueOf(v); }
    private static Long longOf(Object o) { return o instanceof Number n ? n.longValue() : null; }
    private static String now() { return LocalDateTime.now().format(TS); }
}
