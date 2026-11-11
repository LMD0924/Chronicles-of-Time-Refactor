package com.chroniclesoftime.community.interfaces;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.community.application.CommunityApplicationService;
import com.chroniclesoftime.shared.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** 社区域特性：热门、检索、点赞/收藏幂等、文章知识图谱、标签云与关联推荐。 */
@RestController
@RequestMapping("/api/v1/community")
public class CommunityFeatureController {
    private final CommunityApplicationService service;
    public CommunityFeatureController(CommunityApplicationService service) { this.service = service; }

    @GetMapping("/contents/hot")
    public Result<?> hot(@RequestParam(defaultValue = "8") int size) {
        List<Map<String, Object>> list = new ArrayList<>(service.rows("contents"));
        list.sort((a, b) -> Double.compare(num(b.get("viewCount")), num(a.get("viewCount"))));
        return Result.ok(list.subList(0, Math.min(size, list.size())));
    }

    @GetMapping("/contents/search")
    public Result<?> search(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String category) {
        String kw = keyword == null ? "" : keyword.toLowerCase();
        List<Map<String, Object>> out = service.rows("contents").stream().filter(c -> {
            boolean hit = kw.isBlank() || String.valueOf(c.getOrDefault("title", "")).toLowerCase().contains(kw)
                    || String.valueOf(c.getOrDefault("body", "")).toLowerCase().contains(kw)
                    || String.valueOf(c.getOrDefault("tags", "")).toLowerCase().contains(kw);
            boolean cat = category == null || category.isBlank() || category.equals(String.valueOf(c.getOrDefault("category", "")));
            return hit && cat;
        }).toList();
        return Result.ok(out);
    }

    @GetMapping("/contents/{id}/detail")
    public Result<?> detail(@PathVariable Long id) {
        Map<String, Object> raw = service.rows("contents").stream()
                .filter(c -> id.equals(longOf(c.get("id")))).findFirst().orElse(null);
        if (raw == null) return Result.fail(404, "文章不存在");
        Map<String, Object> content = new LinkedHashMap<>(raw);
        content.put("comments", service.rows("comments").stream()
                .filter(c -> id.equals(longOf(c.get("contentId")))).toList());
        content.put("liked", isMarked("likes", id));
        content.put("favorited", isMarked("favorites", id));
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
        for (Map<String, Object> c : service.rows("contents")) {
            String id = "c" + c.get("id");
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", id); n.put("name", c.get("title")); n.put("category", "文章"); n.put("value", num(c.get("viewCount")));
            nodes.put(id, n);
            for (String tag : String.valueOf(c.getOrDefault("tags", "")).split("[,，;；]")) {
                if (tag.isBlank()) continue;
                String tid = "t" + tag.trim();
                nodes.computeIfAbsent(tid, k -> {
                    Map<String, Object> t = new LinkedHashMap<>();
                    t.put("id", tid); t.put("name", tag.trim()); t.put("category", "标签"); t.put("value", 1);
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
        for (Map<String, Object> c : service.rows("contents")) {
            for (String t : String.valueOf(c.getOrDefault("tags", "")).split("[,，;；]")) {
                if (!t.isBlank()) tags.merge(t.trim(), 1, Integer::sum);
            }
        }
        return Result.ok(tags);
    }

    @GetMapping("/category-stats")
    public Result<?> categoryStats() {
        Map<String, Integer> m = new TreeMap<>();
        for (Map<String, Object> c : service.rows("contents")) {
            m.merge(String.valueOf(c.getOrDefault("category", "未分类")), 1, Integer::sum);
        }
        return Result.ok(m);
    }

    @GetMapping("/related")
    public Result<?> related(@RequestParam(required = false) String tags) {
        Set<String> wanted = new HashSet<>(Arrays.asList(String.valueOf(tags == null ? "" : tags).split("[,，;；]")));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> c : service.rows("contents")) {
            long overlap = Arrays.stream(String.valueOf(c.getOrDefault("tags", "")).split("[,，;；]"))
                    .map(String::trim).filter(wanted::contains).count();
            if (overlap > 0) { Map<String, Object> m = new LinkedHashMap<>(c); m.put("overlap", overlap); out.add(m); }
        }
        out.sort((a, b) -> Long.compare((Long) b.get("overlap"), (Long) a.get("overlap")));
        return Result.ok(out);
    }

    private boolean isMarked(String resource, Long contentId) {
        Long uid = AuthContext.userId();
        if (uid == null) return false;
        return service.rows(resource).stream().anyMatch(r -> contentId.equals(longOf(r.get("contentId"))) && uid.equals(longOf(r.get("userId"))));
    }

    private Result<?> mark(String resource, Long contentId, boolean add, String counter) {
        Long uid = AuthContext.userId();
        if (uid == null) return Result.fail(401, "请先登录");
        List<Map<String, Object>> rows = service.rows(resource);
        Optional<Map<String, Object>> existing = rows.stream()
                .filter(r -> contentId.equals(longOf(r.get("contentId"))) && uid.equals(longOf(r.get("userId")))).findFirst();
        if (add && existing.isPresent()) return Result.ok(Map.of("changed", false, "active", true));
        if (add) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("contentId", contentId);
            body.put("createdAt", java.time.LocalDateTime.now().toString().substring(0, 19).replace('T', ' '));
            service.save(resource, body);
        } else if (existing.isPresent()) {
            service.delete(resource, longOf(existing.get().get("id")));
        }
        Map<String, Object> c = service.rows("contents").stream()
                .filter(x -> contentId.equals(longOf(x.get("id")))).findFirst().orElse(null);
        if (c != null) {
            Map<String, Object> patch = new LinkedHashMap<>(c);
            int n = (int) num(patch.get(counter)) + (add ? 1 : -1);
            patch.put(counter, Math.max(0, n));
            service.save("contents", patch);
        }
        return Result.ok(Map.of("changed", true, "active", add));
    }

    private static double num(Object o) { return o instanceof Number n ? n.doubleValue() : 0; }
    private static Long longOf(Object o) { return o instanceof Number n ? n.longValue() : null; }
}
