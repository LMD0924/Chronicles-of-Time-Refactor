package com.chroniclesoftime.identity.domain.repository;

import com.chroniclesoftime.identity.domain.model.AuditLog;
import com.chroniclesoftime.shared.Repository;

/** 身份与访问 · 仓储契约（domain 层定义）。 */
public interface AuditLogRepository extends Repository<AuditLog, Long> {}
