package com.chroniclesoftime.community.interfaces;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.community.application.CommunityApplicationService;
import com.chroniclesoftime.shared.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 社区域特性：热门、检索、详情、点赞 / 收藏幂等、文章知识图谱、标签云与关联推荐。
 * 本类所有端点都在公开 GET 白名单内，因此只能读 status = PUBLISHED 的文章；
 * 草稿 / 待审 / 驳回 / 下架内容仅作者与管理员通过 workflow / admin 端点访问。
 */
@RestController
@RequestMapping("/api/v1/community")
public class CommunityFeatureController {
    private final CommunityApplicationService service;
    public CommunityFeatureController(CommunityApplicationService service) { this.service = service; }

    @GetMapping("/contents/hot")
    public Result<?> hot(@RequestParam(defaultValue = "8") int size) {
        List<Map<String, Object>> list = new ArrayList<>(service.published());
        list.sort((a, b) -> Double.compare(num(b.get("viewCount")), num(a.get("viewCount"))));
        return Result.ok(list.subList(0, Math.min(Math.max(size, 0), list.size())));
    }

    @GetMapping("/contents/search")
    public Result<?> search(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String category,
                            @RequestParam(required = false) String tag,
                            @RequestParam(defaultValue = "newest") String sort) {
        String kw = keyword == null ? "" : keyword.toLowerCase();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> c : service.published()) {
            boolean hit = kw.isBlank()
                    || contains(c.get("title"), kw) || contains(c.get("body"), kw)
                    || contains(c.get("summary"), kw) || contains(c.get("tags"), kw);
            boolean cat = category == null || category.isBlank()
                    || category.equals(String.valueOf(c.getOrDefault("category", "")));
            boolean tagged = tag == null || tag.isBlank() || hasTag(c.get("tags"), tag);
            if (hit && cat && tagged) out.add(c);
        }
        if ("hot".equalsIgnoreCase(sort)) {
            out.sort((a, b) -> Double.compare(num(b.get("viewCount")), num(a.get("viewCount"))));
        } else if ("likes".equalsIgnoreCase(sort)) {
            out.sort((a, b) -> Double.compare(num(b.get("likeCount")), num(a.get("likeCount"))));
        }
        return Result.ok(out);
    }

    @GetMapping("/contents/{id}/detail")
    public Result<?> detail(@PathVariable Long id) {
        Map<String, Object> raw = service.row("contents", id);
        if (raw == null) return Result.fail(404, "文章不存在");
        if (!visible(raw)) return Result.fail(404, "文章不存在或尚未发布");
        Map<String, Object> content = new LinkedHashMap<>(raw);
        content.put("comments", service.findBy("comments", "content_id", id));
        content.put("liked", isMarked("likes", id));
        content.put("favorited", isMarked("favorites", id));
        // 浏览量只对正式发布的内容累加；草稿预览不计入。
        // 用独立的 patch 更新，避免把 comments / liked / favorited 等响应字段写回实体转换。
        if ("PUBLISHED".equals(String.valueOf(raw.get("status")))) {
            int views = (int) num(raw.get("viewCount")) + 1;
            Map<String, Object> patch = new LinkedHashMap<>(raw);
            patch.put("viewCount", views);
            service.saveInternal("contents", patch);
            content.put("viewCount", views);
        }
        return Result.ok(content);
    }

    @PostMapping("/contents/{id}/like")
    public Result<?> like(@PathVariable Long id) { return mark("likes", id, true, "likeCount"); }

    @DeleteMapping("/contents/{id}/like")
    public Result<?> unlike(@PathVariable Long id) { return mark("likes", id, false, "likeCount"); }

    @PostMapping("/contents/{id}/favorite")
    public Result<?> favorite(@PathVariable Long id) { return mark("favorites", id, true, "favoriteCount"); }

    @DeleteMapping("/contents/{id}/favorite")
    public Result<?> unfavorite(@PathVariable Long id) { return mark("favorites", id, false, "favoriteCount"); }

    @GetMapping("/knowledge-graph")
    public Result<?> knowledgeGraph() {
        Map<String, Map<String, Object>> nodes = new LinkedHashMap<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        for (Map<String, Object> c : service.published()) {
            String id = "c" + c.get("id");
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", id); n.put("contentId", c.get("id")); n.put("name", c.get("title"));
            n.put("category", "文章"); n.put("value", num(c.get("viewCount")));
            nodes.put(id, n);
            for (String tag : splitTags(c.get("tags"))) {
                String tid = "t" + tag;
                nodes.computeIfAbsent(tid, k -> {
                    Map<String, Object> t = new LinkedHashMap<>();
                    t.put("id", tid); t.put("name", tag); t.put("category", "标签"); t.put("value", 1);
                    return t;
                });
                edges.add(Map.of("source", id, "target", tid));
            }
        }
        return Result.ok(Map.of("nodes", new ArrayList<>(nodes.values()), "edges", edges));
    }

    @GetMapping("/tag-cloud")
    public Result<?> tagCloud() {
        Map<String, Integer> tags = new TreeMap<>();
        for (Map<String, Object> c : service.published()) {
            for (String t : splitTags(c.get("tags"))) tags.merge(t, 1, Integer::sum);
        }
        return Result.ok(tags);
    }

    @GetMapping("/category-stats")
    public Result<?> categoryStats() {
        Map<String, Integer> m = new TreeMap<>();
        for (Map<String, Object> c : service.published()) {
            m.merge(String.valueOf(c.getOrDefault("category", "未分类")), 1, Integer::sum);
        }
        return Result.ok(m);
    }

    @GetMapping("/related")
    public Result<?> related(@RequestParam(required = false) String tags,
                             @RequestParam(required = false) Long excludeId) {
        Set<String> wanted = new HashSet<>(splitTags(tags));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> c : service.published()) {
            if (excludeId != null && excludeId.equals(longOf(c.get("id")))) continue;
            long overlap = splitTags(c.get("tags")).stream().filter(wanted::contains).count();
            if (overlap > 0) { Map<String, Object> m = new LinkedHashMap<>(c); m.put("overlap", overlap); out.add(m); }
        }
        out.sort((a, b) -> Long.compare(((Number) b.get("overlap")).longValue(), ((Number) a.get("overlap")).longValue()));
        return Result.ok(out);
    }

    /** 游客只能看已发布；登录作者 / 管理员可预览自己的非公开稿件。 */
    private boolean visible(Map<String, Object> c) {
        if ("PUBLISHED".equals(String.valueOf(c.get("status")))) return true;
        Long uid = AuthContext.userId();
        if (uid == null) return false;
        return AuthContext.isAdmin() || uid.equals(longOf(c.get("userId")));
    }

    private boolean isMarked(String resource, Long contentId) {
        Long uid = AuthContext.userId();
        if (uid == null) return false;
        return service.findBy(resource, "content_id", contentId).stream()
                .anyMatch(r -> uid.equals(longOf(r.get("userId"))));
    }

    private Result<?> mark(String resource, Long contentId, boolean add, String counter) {
        Long uid = AuthContext.userId();
        if (uid == null) return Result.fail(401, "请先登录");
        Map<String, Object> c = service.row("contents", contentId);
        if (c == null) return Result.fail(404, "文章不存在");
        if (!"PUBLISHED".equals(String.valueOf(c.get("status")))) return Result.fail(400, "该文章暂不可互动");

        List<Map<String, Object>> rows = service.findBy(resource, "content_id", contentId).stream()
                .filter(r -> uid.equals(longOf(r.get("userId")))).toList();
        if (add && !rows.isEmpty()) return Result.ok(Map.of("changed", false, "active", true));
        if (!add && rows.isEmpty()) return Result.ok(Map.of("changed", false, "active", false));

        if (add) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("contentId", contentId);
            body.put("createdAt", now());
            service.save(resource, body);
        } else {
            service.delete(resource, longOf(rows.get(0).get("id")));
        }
        Map<String, Object> patch = new LinkedHashMap<>(c);
        int n = (int) num(patch.get(counter)) + (add ? 1 : -1);
        patch.put(counter, Math.max(0, n));
        service.saveInternal("contents", patch);
        return Result.ok(Map.of("changed", true, "active", add, counter, Math.max(0, n)));
    }

    private static boolean contains(Object value, String kw) {
        return String.valueOf(value == null ? "" : value).toLowerCase().contains(kw);
    }
    private static boolean hasTag(Object tags, String tag) { return splitTags(tags).contains(tag); }
    private static List<String> splitTags(Object tags) {
        List<String> out = new ArrayList<>();
        for (String t : String.valueOf(tags == null ? "" : tags).split("[,，;；]")) {
            String v = t.trim();
            if (!v.isBlank() && !out.contains(v)) out.add(v);
        }
        return out;
    }
    private static double num(Object o) { return o instanceof Number n ? n.doubleValue() : 0; }
    private static Long longOf(Object o) { return o instanceof Number n ? n.longValue() : null; }
    private static String now() { return java.time.LocalDateTime.now().toString().substring(0, 19).replace('T', ' '); }
}
