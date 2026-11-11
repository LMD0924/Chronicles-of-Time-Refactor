package com.chroniclesoftime.planning.infrastructure.persistence;

import com.chroniclesoftime.planning.domain.model.SelectionHistory;
import com.chroniclesoftime.planning.domain.repository.SelectionHistoryRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class SelectionHistoryRepositoryImpl extends BaseMapperRepository<SelectionHistory, SelectionHistoryMapper> implements SelectionHistoryRepository {
    public SelectionHistoryRepositoryImpl(SelectionHistoryMapper mapper) { super(mapper); }
}
