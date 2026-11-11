package com.chroniclesoftime.identity.infrastructure.persistence;

import com.chroniclesoftime.identity.domain.model.Account;
import com.chroniclesoftime.identity.domain.repository.AccountRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepositoryImpl extends BaseMapperRepository<Account, AccountMapper> implements AccountRepository {
    public AccountRepositoryImpl(AccountMapper mapper) { super(mapper); }
}
