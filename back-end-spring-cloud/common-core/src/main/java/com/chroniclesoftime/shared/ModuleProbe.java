package com.chroniclesoftime.shared;

/** 每个限界上下文实现一个探针，用于验证单启动类是否扫描装配完整。 */
public interface ModuleProbe {
    String moduleName();
    String description();
}
