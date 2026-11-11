package com.chroniclesoftime.community.infrastructure.persistence;

import com.chroniclesoftime.community.domain.model.ContentComment;
import com.chroniclesoftime.community.domain.repository.ContentCommentRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ContentCommentRepositoryImpl extends BaseMapperRepository<ContentComment, ContentCommentMapper> implements ContentCommentRepository {
    public ContentCommentRepositoryImpl(ContentCommentMapper mapper) { super(mapper); }
}
