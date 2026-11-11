package com.chroniclesoftime.community;

import com.chroniclesoftime.shared.ModuleProbe;
import org.springframework.stereotype.Component;

@Component
public class CommunityProbe implements ModuleProbe {
    @Override public String moduleName() { return "community"; }
    @Override public String description() { return "内容发布、列表检索、评论、点赞、收藏与文章知识图谱"; }
}
