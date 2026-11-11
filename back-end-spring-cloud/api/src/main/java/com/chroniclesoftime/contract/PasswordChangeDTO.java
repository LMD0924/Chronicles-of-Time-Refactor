package com.chroniclesoftime.contract;

/** 跨限界上下文共享的 DTO 契约。 */
public class PasswordChangeDTO {
    private String oldPassword;
    private String newPassword;

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String v) { this.oldPassword = v; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String v) { this.newPassword = v; }
}
