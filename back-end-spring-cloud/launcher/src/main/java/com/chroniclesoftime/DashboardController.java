package com.chroniclesoftime;

import com.chroniclesoftime.archive.application.ArchiveApplicationService;
import com.chroniclesoftime.community.application.CommunityApplicationService;
import com.chroniclesoftime.learning.application.LearningApplicationService;
import com.chroniclesoftime.planning.application.PlanningApplicationService;
import com.chroniclesoftime.shared.Result;
import com.chroniclesoftime.timeline.application.TimelineApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/** 首页公开概览：跨域统计，给 4 个彩色圆形指标与进度条供数。 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final TimelineApplicationService timeline;
    private final LearningApplicationService learning;
    private final PlanningApplicationService planning;
    private final CommunityApplicationService community;
    private final ArchiveApplicationService archive;

    public DashboardController(TimelineApplicationService timeline, LearningApplicationService learning,
                               PlanningApplicationService planning, CommunityApplicationService community,
                               ArchiveApplicationService archive) {
        this.timeline = timeline; this.learning = learning; this.planning = planning;
        this.community = community; this.archive = archive;
    }

    @GetMapping
    public Result<?> dashboard() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("records", timeline.rows("records").size());
        m.put("milestones", timeline.rows("milestones").size());
        m.put("questions", learning.rows("questions").size());
        m.put("scores", learning.rows("scores").size());
        m.put("plans", planning.rows("volunteer-plans").size() + planning.rows("selections").size());
        m.put("contents", community.rows("contents").size());
        m.put("certificates", archive.rows("certificates").size());
        m.put("links", timeline.rows("links").size());
        Map<String, Object> modules = new LinkedHashMap<>();
        modules.put("timeline", timeline.stats());
        modules.put("learning", learning.stats());
        modules.put("planning", planning.stats());
        modules.put("community", community.stats());
        modules.put("archive", archive.stats());
        m.put("modules", modules);
        return Result.ok(m);
    }
}
