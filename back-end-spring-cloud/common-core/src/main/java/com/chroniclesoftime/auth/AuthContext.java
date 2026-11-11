package com.chroniclesoftime.auth;

/** 请求级身份上下文，业务服务通过它取当前用户，不反向依赖 identity 模块。 */
public final class AuthContext {
    private static final ThreadLocal<AuthUser> HOLDER = new ThreadLocal<>();
    private AuthContext() {}

    public static void set(AuthUser user) { HOLDER.set(user); }
    public static AuthUser get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }
    public static Long userId() { AuthUser u = HOLDER.get(); return u == null ? null : u.getUserId(); }
    public static String username() { AuthUser u = HOLDER.get(); return u == null ? null : u.getUsername(); }
    public static String role() { AuthUser u = HOLDER.get(); return u == null ? null : u.getRole(); }
    public static boolean isAdmin() { return "ADMIN".equals(role()); }
}
