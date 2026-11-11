package com.chroniclesoftime.identity.interfaces;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.auth.JwtUtil;
import com.chroniclesoftime.auth.PasswordHasher;
import com.chroniclesoftime.identity.domain.model.Account;
import com.chroniclesoftime.identity.domain.repository.AccountRepository;
import com.chroniclesoftime.shared.Result;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 登录 / 注册 / 退出 / 校验：登录失败计数与 30 分钟冻结、token_version 主动失效。 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final int MAX_FAIL = 5;
    private static final long LOCK_MS = 30L * 60 * 1000;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AccountRepository accounts;
    private final JwtUtil jwt;

    public AuthController(AccountRepository accounts, JwtUtil jwt) {
        this.accounts = accounts; this.jwt = jwt;
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, Object> body) {
        String username = str(body.get("username"));
        String password = str(body.get("password"));
        List<Account> found = accounts.findBy("username", username);
        Account acc = found.isEmpty() ? null : found.get(0);
        if (acc == null || username.isBlank()) {
            return Result.fail(401, "账号或密码错误，还可尝试 4 次");
        }
        if (acc.getLockedUntil() != null && acc.getLockedUntil() > System.currentTimeMillis()) {
            return Result.fail(423, "账号已冻结，请稍后再试");
        }
        if (!PasswordHasher.matches(password, acc.getPasswordHash())) {
            int fails = (acc.getLoginFailCount() == null ? 0 : acc.getLoginFailCount()) + 1;
            acc.setLoginFailCount(fails);
            if (fails >= MAX_FAIL) {
                acc.setLockedUntil(System.currentTimeMillis() + LOCK_MS);
                acc.setLoginFailCount(0);
            }
            accounts.save(acc);
            return fails >= MAX_FAIL
                    ? Result.fail(423, "连续失败 5 次，账号冻结 30 分钟")
                    : Result.fail(401, "账号或密码错误，还可尝试 " + (MAX_FAIL - fails) + " 次");
        }
        if (!"ACTIVE".equals(acc.getStatus())) return Result.fail(403, "账号已停用");
        acc.setLoginFailCount(0);
        acc.setLockedUntil(null);
        acc.setLastLoginAt(now());
        accounts.save(acc);
        int ver = acc.getTokenVersion() == null ? 1 : acc.getTokenVersion();
        String token = jwt.issue(acc.getId(), acc.getUsername(), acc.getRole(), ver);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", token); out.put("userId", acc.getId()); out.put("username", acc.getUsername());
        out.put("nickname", acc.getNickname()); out.put("role", acc.getRole());
        return Result.ok(out);
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, Object> body) {
        String username = str(body.get("username"));
        String password = str(body.get("password"));
        if (username.isBlank() || username.length() < 3) return Result.fail(400, "账号至少 3 个字符");
        if (password == null || password.length() < 6) return Result.fail(400, "密码至少 6 个字符");
        if (!accounts.findBy("username", username).isEmpty()) return Result.fail(409, "账号已存在");
        Account acc = new Account();
        acc.setUsername(username);
        acc.setPasswordHash(PasswordHasher.hash(password));
        acc.setNickname(str(body.get("nickname")).isBlank() ? username : str(body.get("nickname")));
        acc.setRole("USER");                 // 自助注册强制普通用户，杜绝匿名提权
        acc.setStatus("ACTIVE");
        acc.setTokenVersion(1);
        acc.setLoginFailCount(0);
        acc.setCreatedAt(now());
        Account saved = accounts.save(acc);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", saved.getId()); out.put("username", saved.getUsername()); out.put("role", "USER");
        return Result.ok(out);
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        Long uid = AuthContext.userId();
        if (uid != null) {
            Account acc = accounts.findById(uid);
            if (acc != null) {
                acc.setTokenVersion((acc.getTokenVersion() == null ? 1 : acc.getTokenVersion()) + 1);
                accounts.save(acc);
            }
        }
        return Result.ok();
    }

    @GetMapping("/verify")
    public Result<?> verify(@RequestHeader(value = "Authorization", required = false) String auth) {
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
        Map<String, Object> payload = jwt.verify(token);
        if (payload == null) return Result.fail(401, "登录已失效");
        return Result.ok(payload);
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private static String now() { return LocalDateTime.now().format(FMT); }
}
