package com.chroniclesoftime.timeline;

import com.chroniclesoftime.shared.ModuleProbe;
import org.springframework.stereotype.Component;

@Component
public class TimelineProbe implements ModuleProbe {
    @Override public String moduleName() { return "timeline"; }
    @Override public String description() { return "成长记录、里程碑、贯通关联、工作与进阶记录"; }
}
