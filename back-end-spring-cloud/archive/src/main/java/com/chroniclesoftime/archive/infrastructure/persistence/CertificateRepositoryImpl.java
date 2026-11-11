package com.chroniclesoftime.archive.infrastructure.persistence;

import com.chroniclesoftime.archive.domain.model.Certificate;
import com.chroniclesoftime.archive.domain.repository.CertificateRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class CertificateRepositoryImpl extends BaseMapperRepository<Certificate, CertificateMapper> implements CertificateRepository {
    public CertificateRepositoryImpl(CertificateMapper mapper) { super(mapper); }
}
