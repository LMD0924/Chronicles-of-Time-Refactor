package com.chroniclesoftime.archive.infrastructure.persistence;

import com.chroniclesoftime.archive.domain.model.ThesisBoardItem;
import com.chroniclesoftime.archive.domain.repository.ThesisBoardItemRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ThesisBoardItemRepositoryImpl extends BaseMapperRepository<ThesisBoardItem, ThesisBoardItemMapper> implements ThesisBoardItemRepository {
    public ThesisBoardItemRepositoryImpl(ThesisBoardItemMapper mapper) { super(mapper); }
}
