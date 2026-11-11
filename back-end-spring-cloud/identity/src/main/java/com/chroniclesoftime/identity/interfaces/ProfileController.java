package com.chroniclesoftime.identity.interfaces;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.auth.PasswordHasher;
import com.chroniclesoftime.identity.application.IdentityApplicationService;
import com.chroniclesoftime.identity.domain.model.Account;
import com.chroniclesoftime.identity.domain.model.UserProfile;
import com.chroniclesoftime.identity.domain.repository.AccountRepository;
import com.chroniclesoftime.identity.domain.repository.UserProfileRepository;
import com.chroniclesoftime.shared.Result;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 个人中心：资料编辑、改密码（吊销旧会话）、我的数据概览。 */
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final AccountRepository accounts;
    private final UserProfileRepository profiles;
    private final IdentityApplicationService service;

    public ProfileController(AccountRepository accounts, UserProfileRepository profiles,
                             IdentityApplicationService service) {
        this.accounts = accounts; this.profiles = profiles; this.service = service;
    }

    @GetMapping
    public Result<?> me() {
        Long uid = AuthContext.userId();
        if (uid == null) return Result.fail(401, "未登录");
        Account acc = accounts.findById(uid);
        List<UserProfile> list = profiles.findBy("user_id", uid);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("account", acc);
        out.put("profile", list.isEmpty() ? null : list.get(0));
        return Result.ok(out);
    }

    @PutMapping
    public Result<?> update(@RequestBody Map<String, Object> body) {
        Long uid = AuthContext.userId();
        if (uid == null) return Result.fail(401, "未登录");
        Account acc = accounts.findById(uid);
        if (acc != null) {
            if (body.containsKey("nickname")) acc.setNickname(String.valueOf(body.get("nickname")));
            if (body.containsKey("bio")) acc.setBio(String.valueOf(body.get("bio")));
            if (body.containsKey("region")) acc.setRegion(String.valueOf(body.get("region")));
            if (body.containsKey("avatarUrl")) acc.setAvatarUrl(String.valueOf(body.get("avatarUrl")));
            accounts.save(acc);
        }
        List<UserProfile> list = profiles.findBy("user_id", uid);
        UserProfile profile = list.isEmpty() ? new UserProfile() : list.get(0);
        if (profile.getUserId() == null) profile.setUserId(uid);
        for (String f : List.of("realName", "phone", "email", "school", "bio", "birthday")) {
            if (body.containsKey(f)) setField(profile, f, String.valueOf(body.get(f)));
        }
        profile.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return Result.ok(profiles.save(profile));
    }

    @PostMapping("/password")
    public Result<?> changePassword(@RequestBody Map<String, Object> body) {
        Long uid = AuthContext.userId();
        if (uid == null) return Result.fail(401, "未登录");
        Account acc = accounts.findById(uid);
        if (acc == null) return Result.fail(404, "账号不存在");
        if (!PasswordHasher.matches(String.valueOf(body.get("oldPassword")), acc.getPasswordHash())) {
            return Result.fail(400, "原密码不正确");
        }
        String next = String.valueOf(body.get("newPassword"));
        if (next.length() < 6) return Result.fail(400, "新密码至少 6 个字符");
        acc.setPasswordHash(PasswordHasher.hash(next));
        acc.setTokenVersion((acc.getTokenVersion() == null ? 1 : acc.getTokenVersion()) + 1);
        accounts.save(acc);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("changed", true); out.put("reloginRequired", true);
        return Result.ok(out);
    }

    @GetMapping("/overview")
    public Result<?> overview() {
        Long uid = AuthContext.userId();
        if (uid == null) return Result.fail(401, "未登录");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", uid);
        m.put("role", AuthContext.role());
        m.put("identity", service.stats());
        return Result.ok(m);
    }

    private void setField(UserProfile p, String f, String v) {
        switch (f) {
            case "realName" -> p.setRealName(v);
            case "phone" -> p.setPhone(v);
            case "email" -> p.setEmail(v);
            case "school" -> p.setSchool(v);
            case "bio" -> p.setBio(v);
            case "birthday" -> p.setBirthday(v);
            default -> { }
        }
    }
}
