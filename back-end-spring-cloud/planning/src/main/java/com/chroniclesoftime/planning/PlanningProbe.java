package com.chroniclesoftime.planning;

import com.chroniclesoftime.shared.ModuleProbe;
import org.springframework.stereotype.Component;

@Component
public class PlanningProbe implements ModuleProbe {
    @Override public String moduleName() { return "planning"; }
    @Override public String description() { return "选科、志愿、学科等级、课程与学分、复习与目标规划"; }
}
