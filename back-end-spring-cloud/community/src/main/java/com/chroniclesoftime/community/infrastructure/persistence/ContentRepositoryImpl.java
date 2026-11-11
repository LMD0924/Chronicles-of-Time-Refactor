package com.chroniclesoftime.community.infrastructure.persistence;

import com.chroniclesoftime.community.domain.model.Content;
import com.chroniclesoftime.community.domain.repository.ContentRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ContentRepositoryImpl extends BaseMapperRepository<Content, ContentMapper> implements ContentRepository {
    public ContentRepositoryImpl(ContentMapper mapper) { super(mapper); }
}
