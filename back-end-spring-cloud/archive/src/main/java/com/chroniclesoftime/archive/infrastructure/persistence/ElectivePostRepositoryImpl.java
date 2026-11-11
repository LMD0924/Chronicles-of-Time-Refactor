package com.chroniclesoftime.archive.infrastructure.persistence;

import com.chroniclesoftime.archive.domain.model.ElectivePost;
import com.chroniclesoftime.archive.domain.repository.ElectivePostRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ElectivePostRepositoryImpl extends BaseMapperRepository<ElectivePost, ElectivePostMapper> implements ElectivePostRepository {
    public ElectivePostRepositoryImpl(ElectivePostMapper mapper) { super(mapper); }
}
