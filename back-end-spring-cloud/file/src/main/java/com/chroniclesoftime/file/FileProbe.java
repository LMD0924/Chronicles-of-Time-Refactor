package com.chroniclesoftime.file;

import com.chroniclesoftime.shared.ModuleProbe;
import org.springframework.stereotype.Component;

@Component
public class FileProbe implements ModuleProbe {
    @Override public String moduleName() { return "file"; }
    @Override public String description() { return "图片 / 文件 / 批量上传与删除"; }
}
