package com.chroniclesoftime.learning;

import com.chroniclesoftime.shared.ModuleProbe;
import org.springframework.stereotype.Component;

@Component
public class LearningProbe implements ModuleProbe {
    @Override public String moduleName() { return "learning"; }
    @Override public String description() { return "题库、练习、答题记录、错题本、成绩分析与知识图谱"; }
}
