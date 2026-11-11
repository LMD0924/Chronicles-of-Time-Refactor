package com.chroniclesoftime.auth;

/** common-core 定义端口，identity 模块实现，避免基础设施反向依赖业务模块。 */
public interface AccountLookup {
    AccountSnapshot findByUsername(String username);
    AccountSnapshot findById(Long id);

    class AccountSnapshot {
        private Long id;
        private String username;
        private String passwordHash;
        private String role;
        private String status;
        private int tokenVersion;
        private int loginFailCount;
        private Long lockedUntil;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPasswordHash() { return passwordHash; }
        public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getTokenVersion() { return tokenVersion; }
        public void setTokenVersion(int tokenVersion) { this.tokenVersion = tokenVersion; }
        public int getLoginFailCount() { return loginFailCount; }
        public void setLoginFailCount(int loginFailCount) { this.loginFailCount = loginFailCount; }
        public Long getLockedUntil() { return lockedUntil; }
        public void setLockedUntil(Long lockedUntil) { this.lockedUntil = lockedUntil; }
    }
}
