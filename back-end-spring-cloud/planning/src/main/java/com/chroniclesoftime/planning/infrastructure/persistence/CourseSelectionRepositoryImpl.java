package com.chroniclesoftime.planning.infrastructure.persistence;

import com.chroniclesoftime.planning.domain.model.CourseSelection;
import com.chroniclesoftime.planning.domain.repository.CourseSelectionRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class CourseSelectionRepositoryImpl extends BaseMapperRepository<CourseSelection, CourseSelectionMapper> implements CourseSelectionRepository {
    public CourseSelectionRepositoryImpl(CourseSelectionMapper mapper) { super(mapper); }
}
