package com.chroniclesoftime.archive.infrastructure.persistence;

import com.chroniclesoftime.archive.domain.model.PaperSuggestion;
import com.chroniclesoftime.archive.domain.repository.PaperSuggestionRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class PaperSuggestionRepositoryImpl extends BaseMapperRepository<PaperSuggestion, PaperSuggestionMapper> implements PaperSuggestionRepository {
    public PaperSuggestionRepositoryImpl(PaperSuggestionMapper mapper) { super(mapper); }
}
