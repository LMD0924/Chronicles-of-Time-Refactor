package com.chroniclesoftime.archive.infrastructure.persistence;

import com.chroniclesoftime.archive.domain.model.CourseTreeNode;
import com.chroniclesoftime.archive.domain.repository.CourseTreeNodeRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class CourseTreeNodeRepositoryImpl extends BaseMapperRepository<CourseTreeNode, CourseTreeNodeMapper> implements CourseTreeNodeRepository {
    public CourseTreeNodeRepositoryImpl(CourseTreeNodeMapper mapper) { super(mapper); }
}
