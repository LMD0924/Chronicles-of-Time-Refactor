package com.chroniclesoftime.identity.infrastructure;

import com.chroniclesoftime.auth.AccountLookup;
import com.chroniclesoftime.identity.domain.model.Account;
import com.chroniclesoftime.identity.domain.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/** 认证基础设施查账号的适配器，实现 common-core 定义的端口。 */
@Component
public class AccountLookupImpl implements AccountLookup {
    private final AccountRepository accounts;
    public AccountLookupImpl(AccountRepository accounts) { this.accounts = accounts; }

    @Override
    public AccountSnapshot findByUsername(String username) {
        List<Account> found = accounts.findBy("username", username);
        return found.isEmpty() ? null : toSnapshot(found.get(0));
    }

    @Override
    public AccountSnapshot findById(Long id) {
        Account a = accounts.findById(id);
        return a == null ? null : toSnapshot(a);
    }

    private AccountSnapshot toSnapshot(Account a) {
        AccountSnapshot s = new AccountSnapshot();
        s.setId(a.getId()); s.setUsername(a.getUsername()); s.setPasswordHash(a.getPasswordHash());
        s.setRole(a.getRole()); s.setStatus(a.getStatus());
        s.setTokenVersion(a.getTokenVersion() == null ? 1 : a.getTokenVersion());
        s.setLoginFailCount(a.getLoginFailCount() == null ? 0 : a.getLoginFailCount());
        s.setLockedUntil(a.getLockedUntil());
        return s;
    }
}
