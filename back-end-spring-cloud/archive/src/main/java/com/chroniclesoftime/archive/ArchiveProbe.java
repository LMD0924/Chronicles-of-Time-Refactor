package com.chroniclesoftime.archive;

import com.chroniclesoftime.shared.ModuleProbe;
import org.springframework.stereotype.Component;

@Component
public class ArchiveProbe implements ModuleProbe {
    @Override public String moduleName() { return "archive"; }
    @Override public String description() { return "个人档案、简历、证书、课程树、论文与选课参考"; }
}
