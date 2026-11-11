package com.chroniclesoftime.planning.infrastructure.persistence;

import com.chroniclesoftime.planning.domain.model.CourseGuidance;
import com.chroniclesoftime.planning.domain.repository.CourseGuidanceRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class CourseGuidanceRepositoryImpl extends BaseMapperRepository<CourseGuidance, CourseGuidanceMapper> implements CourseGuidanceRepository {
    public CourseGuidanceRepositoryImpl(CourseGuidanceMapper mapper) { super(mapper); }
}
