package com.chroniclesoftime;

import com.chroniclesoftime.shared.ModuleProbe;
import com.chroniclesoftime.shared.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {
    private final List<ModuleProbe> probes;

    public SystemController(List<ModuleProbe> probes) { this.probes = probes; }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "UP");
        m.put("application", "chronicles-of-time");
        return Result.ok(m);
    }

    @GetMapping("/modules")
    public Result<List<Map<String, String>>> modules() {
        return Result.ok(probes.stream().map(p -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("name", p.moduleName()); m.put("description", p.description());
            return m;
        }).toList());
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("application", "拾光记 · Chronicles of Time");
        m.put("boundedContexts", probes.size());
        m.put("architecture", "混合 DDD 模块化单体");
        m.put("runtime", "Spring Boot 3.5.11 / Spring Cloud 2025.0.1 (依赖保留，默认关闭)");
        return Result.ok(m);
    }
}
