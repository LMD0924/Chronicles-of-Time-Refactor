package com.chroniclesoftime.identity.infrastructure.persistence;

import com.chroniclesoftime.identity.domain.model.UserProfile;
import com.chroniclesoftime.identity.domain.repository.UserProfileRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class UserProfileRepositoryImpl extends BaseMapperRepository<UserProfile, UserProfileMapper> implements UserProfileRepository {
    public UserProfileRepositoryImpl(UserProfileMapper mapper) { super(mapper); }
}
