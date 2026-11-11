package com.chroniclesoftime.identity.infrastructure.persistence;

import com.chroniclesoftime.identity.domain.model.AuditLog;
import com.chroniclesoftime.identity.domain.repository.AuditLogRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepositoryImpl extends BaseMapperRepository<AuditLog, AuditLogMapper> implements AuditLogRepository {
    public AuditLogRepositoryImpl(AuditLogMapper mapper) { super(mapper); }
}
