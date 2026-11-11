package com.chroniclesoftime.archive.infrastructure.persistence;

import com.chroniclesoftime.archive.domain.model.Paper;
import com.chroniclesoftime.archive.domain.repository.PaperRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class PaperRepositoryImpl extends BaseMapperRepository<Paper, PaperMapper> implements PaperRepository {
    public PaperRepositoryImpl(PaperMapper mapper) { super(mapper); }
}
