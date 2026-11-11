package com.chroniclesoftime.planning.infrastructure.persistence;

import com.chroniclesoftime.planning.domain.model.ChecklistItem;
import com.chroniclesoftime.planning.domain.repository.ChecklistItemRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ChecklistItemRepositoryImpl extends BaseMapperRepository<ChecklistItem, ChecklistItemMapper> implements ChecklistItemRepository {
    public ChecklistItemRepositoryImpl(ChecklistItemMapper mapper) { super(mapper); }
}
