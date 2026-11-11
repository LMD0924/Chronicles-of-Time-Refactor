package com.chroniclesoftime.learning.interfaces;

import com.chroniclesoftime.learning.application.LearningApplicationService;
import com.chroniclesoftime.shared.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** 学习 · 统一资源接口（列表 / 详情 / 新增 / 修改 / 删除 / 批量删除 / 统计）。 */
@RestController
@RequestMapping("/api/v1/learning/r")
public class LearningController {
    private final LearningApplicationService service;
    public LearningController(LearningApplicationService service) { this.service = service; }

    @GetMapping
    public Result<Set<String>> resources() { return Result.ok(service.resources()); }

    @GetMapping("/meta/stats")
    public Result<Map<String, Object>> stats() { return Result.ok(service.stats()); }

    @GetMapping("/{resource}")
    public Result<List<Object>> list(@PathVariable String resource,
                                     @RequestParam(required = false, defaultValue = "false") boolean all,
                                     @RequestParam(required = false) String keyword) {
        return Result.ok(service.list(resource, all, keyword));
    }

    @GetMapping("/{resource}/{id}")
    public Result<Object> get(@PathVariable String resource, @PathVariable Long id) {
        return Result.ok(service.get(resource, id));
    }

    @PostMapping("/{resource}")
    public Result<Object> save(@PathVariable String resource, @RequestBody Map<String, Object> body) {
        return Result.ok(service.save(resource, body));
    }

    @PutMapping("/{resource}/{id}")
    public Result<Object> update(@PathVariable String resource, @PathVariable Long id,
                                 @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return Result.ok(service.save(resource, body));
    }

    @DeleteMapping("/{resource}/{id}")
    public Result<Boolean> delete(@PathVariable String resource, @PathVariable Long id) {
        return Result.ok(service.delete(resource, id));
    }

    @PostMapping("/{resource}/batch-delete")
    public Result<Integer> batchDelete(@PathVariable String resource, @RequestBody List<Long> ids) {
        int n = 0;
        for (Long id : ids) if (service.delete(resource, id)) n++;
        return Result.ok(n);
    }
}
