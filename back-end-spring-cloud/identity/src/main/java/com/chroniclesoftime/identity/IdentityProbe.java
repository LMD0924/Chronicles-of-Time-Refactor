package com.chroniclesoftime.identity;

import com.chroniclesoftime.shared.ModuleProbe;
import org.springframework.stereotype.Component;

@Component
public class IdentityProbe implements ModuleProbe {
    @Override public String moduleName() { return "identity"; }
    @Override public String description() { return "认证、用户、角色、个人中心、管理后台与操作审计"; }
}
