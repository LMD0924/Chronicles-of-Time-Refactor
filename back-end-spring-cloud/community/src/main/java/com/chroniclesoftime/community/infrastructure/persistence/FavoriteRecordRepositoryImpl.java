package com.chroniclesoftime.community.infrastructure.persistence;

import com.chroniclesoftime.community.domain.model.FavoriteRecord;
import com.chroniclesoftime.community.domain.repository.FavoriteRecordRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class FavoriteRecordRepositoryImpl extends BaseMapperRepository<FavoriteRecord, FavoriteRecordMapper> implements FavoriteRecordRepository {
    public FavoriteRecordRepositoryImpl(FavoriteRecordMapper mapper) { super(mapper); }
}
