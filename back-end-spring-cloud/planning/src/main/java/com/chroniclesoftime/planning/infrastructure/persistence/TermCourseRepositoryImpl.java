package com.chroniclesoftime.planning.infrastructure.persistence;

import com.chroniclesoftime.planning.domain.model.TermCourse;
import com.chroniclesoftime.planning.domain.repository.TermCourseRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class TermCourseRepositoryImpl extends BaseMapperRepository<TermCourse, TermCourseMapper> implements TermCourseRepository {
    public TermCourseRepositoryImpl(TermCourseMapper mapper) { super(mapper); }
}
