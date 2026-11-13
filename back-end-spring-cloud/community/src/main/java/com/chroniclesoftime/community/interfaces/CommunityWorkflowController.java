package com.chroniclesoftime.community.interfaces;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.community.application.CommunityApplicationService;
import com.chroniclesoftime.shared.Result;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 社区 · 投稿与评论工作流（D4：草稿 → 管理员审核 → 发布 / 驳回）。
 * 全部端点都需要登录；公开只读聚合仍在 CommunityFeatureController。
 * 状态机：DRAFT → PENDING → PUBLISHED / REJECTED；PENDING 可撤回，REJECTED 可改稿重投。
 */
@RestController
@RequestMapping("/api/v1/community/workflow")
public class CommunityWorkflowController {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> STAGES = List.of("DRAFT", "PENDING", "PUBLISHED", "REJECTED", "HIDDEN", "ARCHIVED");
    private static final List<String> EDITABLE_FIELDS = List.of("title", "body", "summary", "category", "tags", "coverUrl");

    private final CommunityApplicationService service;
    public CommunityWorkflowController(CommunityApplicationService service) { this.service = service; }

    /** 我的投稿：含审核理由与审核时间，按 id 倒序。 */
    @GetMapping("/my-contents")
    public Result<?> myContents(@RequestParam(required = false) String status) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> c : service.rowsScoped("contents")) {
            if (status == null || status.isBlank() || status.equals(String.valueOf(c.get("status")))) out.add(c);
        }
        return Result.ok(Map.of("items", out, "stages", STAGES));
    }

    @GetMapping("/overview")
    public Result<?> overview() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String s : STAGES) counts.put(s, 0);
        int total = 0;
        for (Map<String, Object> c : service.rowsScoped("contents")) {
            String s = String.valueOf(c.getOrDefault("status", "DRAFT"));
            counts.merge(s, 1, Integer::sum);
            total++;
        }
        return Result.ok(Map.of("counts", counts, "total", total, "stages", STAGES));
    }

    /** 作者预览自己的任意状态稿件。 */
    @GetMapping("/contents/{id}")
    public Result<?> myContent(@PathVariable Long id) {
        Map<String, Object> c = service.row("contents", id);
        if (c == null || !owned(c)) return Result.fail(404, "稿件不存在");
        return Result.ok(c);
    }

    /** 新建投稿：只允许存草稿或直接提交审核，不能绕过审核直接发布。 */
    @PostMapping("/contents")
    public Result<?> create(@RequestBody Map<String, Object> body) {
        String title = text(body.get("title")).trim();
        String content = text(body.get("body")).trim();
        if (title.isBlank() || content.isBlank()) return Result.fail(400, "标题与正文不能为空");
        String status = text(body.get("status")).toUpperCase();
        if (!status.equals("PENDING")) status = "DRAFT";

        String plain = content.replaceAll("[#*`>\\-]", "");
        Map<String, Object> article = new LinkedHashMap<>();
        article.put("title", title);
        article.put("body", content);
        article.put("summary", text(body.get("summary")).isBlank()
                ? plain.substring(0, Math.min(120, plain.length()))
                : text(body.get("summary")));
        article.put("category", text(body.get("category")).isBlank() ? "随笔" : text(body.get("category")));
        article.put("tags", text(body.get("tags")));
        article.put("coverUrl", text(body.get("coverUrl")));
        article.put("authorName", AuthContext.username());
        article.put("status", status);
        article.put("viewCount", 0);
        article.put("likeCount", 0);
        article.put("favoriteCount", 0);
        article.put("reviewNote", "");
        article.put("createdAt", now());
        article.put("updatedAt", now());
        return Result.ok(service.save("contents", article));
    }

    /** 改稿：仅草稿 / 驳回稿可改，改完可选直接送审。 */
    @PutMapping("/contents/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> c = service.row("contents", id);
        if (c == null || !owned(c)) return Result.fail(404, "稿件不存在");
        String status = String.valueOf(c.get("status"));
        if (!"DRAFT".equals(status) && !"REJECTED".equals(status)) {
            return Result.fail(400, "当前状态不可编辑，待审核请先撤回，已发布请先下架");
        }
        Map<String, Object> patch = new LinkedHashMap<>(c);
        for (String key : EDITABLE_FIELDS) if (body.containsKey(key)) patch.put(key, body.get(key));
        if (text(patch.get("title")).isBlank() || text(patch.get("body")).isBlank()) {
            return Result.fail(400, "标题与正文不能为空");
        }
        boolean submit = Boolean.TRUE.equals(body.get("submit"));
        patch.put("status", submit ? "PENDING" : "DRAFT");
        patch.put("reviewNote", "");
        patch.put("updatedAt", now());
        return Result.ok(service.saveInternal("contents", patch));
    }

    /** 提交审核：DRAFT / REJECTED → PENDING，并清空上次的驳回理由。 */
    @PostMapping("/contents/{id}/submit")
    public Result<?> submit(@PathVariable Long id) {
        Map<String, Object> c = service.row("contents", id);
        if (c == null || !owned(c)) return Result.fail(404, "稿件不存在");
        String status = String.valueOf(c.get("status"));
        if (!"DRAFT".equals(status) && !"REJECTED".equals(status)) {
            return Result.fail(400, "只有草稿或被驳回的稿件可以提交审核");
        }
        Map<String, Object> patch = new LinkedHashMap<>(c);
        patch.put("status", "PENDING");
        patch.put("reviewNote", "");
        patch.put("updatedAt", now());
        service.saveInternal("contents", patch);
        return Result.ok(Map.of("id", id, "status", "PENDING"));
    }

    /** 撤回送审：PENDING → DRAFT。 */
    @PostMapping("/contents/{id}/withdraw")
    public Result<?> withdraw(@PathVariable Long id) {
        Map<String, Object> c = service.row("contents", id);
        if (c == null || !owned(c)) return Result.fail(404, "稿件不存在");
        if (!"PENDING".equals(String.valueOf(c.get("status")))) return Result.fail(400, "只有待审核的稿件可以撤回");
        Map<String, Object> patch = new LinkedHashMap<>(c);
        patch.put("status", "DRAFT");
        patch.put("updatedAt", now());
        service.saveInternal("contents", patch);
        return Result.ok(Map.of("id", id, "status", "DRAFT"));
    }

    /** 删除自己的稿件，并清理该文章的评论 / 点赞 / 收藏。 */
    @DeleteMapping("/contents/{id}")
    public Result<?> remove(@PathVariable Long id) {
        Map<String, Object> c = service.row("contents", id);
        if (c == null || !owned(c)) return Result.fail(404, "稿件不存在");
        int cleaned = 0;
        for (String resource : new String[]{"comments", "likes", "favorites"}) {
            for (Map<String, Object> r : service.findBy(resource, "content_id", id)) {
                if (service.deleteInternal(resource, longOf(r.get("id")))) cleaned++;
            }
        }
        service.deleteInternal("contents", id);
        return Result.ok(Map.of("id", id, "deleted", true, "cleaned", cleaned));
    }

    /** 发表评论：只允许评论已发布文章，作者名取当前登录账号。 */
    @PostMapping("/contents/{id}/comments")
    public Result<?> addComment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> c = service.row("contents", id);
        if (c == null) return Result.fail(404, "文章不存在");
        if (!"PUBLISHED".equals(String.valueOf(c.get("status")))) return Result.fail(400, "该文章暂不可评论");
        String text = text(body.get("body")).trim();
        if (text.isBlank()) return Result.fail(400, "评论内容不能为空");
        if (text.length() > 1000) return Result.fail(400, "评论最多 1000 字");
        Map<String, Object> comment = new LinkedHashMap<>();
        comment.put("contentId", id);
        comment.put("body", text);
        comment.put("authorName", AuthContext.username());
        comment.put("createdAt", now());
        Object saved = service.save("comments", comment);
        return Result.ok(saved);
    }

    /** 删除评论：仅评论作者或管理员。 */
    @DeleteMapping("/comments/{id}")
    public Result<?> deleteComment(@PathVariable Long id) {
        Map<String, Object> comment = service.row("comments", id);
        if (comment == null) return Result.fail(404, "评论不存在");
        if (!owned(comment)) return Result.fail(403, "只能删除自己的评论");
        return Result.ok(Map.of("deleted", service.delete("comments", id)));
    }

    private boolean owned(Map<String, Object> row) {
        Long uid = AuthContext.userId();
        if (uid == null) return false;
        return AuthContext.isAdmin() || uid.equals(longOf(row.get("userId")));
    }
    private static String text(Object v) { return v == null ? "" : String.valueOf(v); }
    private static Long longOf(Object o) { return o instanceof Number n ? n.longValue() : null; }
    private static String now() { return LocalDateTime.now().format(TS); }
}
