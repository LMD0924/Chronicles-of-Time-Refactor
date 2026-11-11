package com.chroniclesoftime.identity.infrastructure.persistence;

import com.chroniclesoftime.identity.domain.model.ResumeSection;
import com.chroniclesoftime.identity.domain.repository.ResumeSectionRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ResumeSectionRepositoryImpl extends BaseMapperRepository<ResumeSection, ResumeSectionMapper> implements ResumeSectionRepository {
    public ResumeSectionRepositoryImpl(ResumeSectionMapper mapper) { super(mapper); }
}
