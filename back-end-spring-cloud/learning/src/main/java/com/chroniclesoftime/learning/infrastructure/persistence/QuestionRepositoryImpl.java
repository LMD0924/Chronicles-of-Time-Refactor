package com.chroniclesoftime.learning.infrastructure.persistence;

import com.chroniclesoftime.learning.domain.model.Question;
import com.chroniclesoftime.learning.domain.repository.QuestionRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionRepositoryImpl extends BaseMapperRepository<Question, QuestionMapper> implements QuestionRepository {
    public QuestionRepositoryImpl(QuestionMapper mapper) { super(mapper); }
}
