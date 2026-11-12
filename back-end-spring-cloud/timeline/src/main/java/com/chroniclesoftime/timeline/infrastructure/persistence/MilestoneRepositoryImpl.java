package com.chroniclesoftime.timeline.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.chroniclesoftime.timeline.domain.model.Milestone;
import com.chroniclesoftime.timeline.domain.repository.MilestoneRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MilestoneRepositoryImpl extends BaseMapperRepository<Milestone, MilestoneMapper> implements MilestoneRepository {
    public MilestoneRepositoryImpl(MilestoneMapper mapper) { super(mapper); }

    @Override
    public void clearLinkedRecord(Long id) {
        mapper.update(null, new UpdateWrapper<Milestone>()
                .eq("id", id)
                .set("linked_record_id", null)
                .set("origin", "MANUAL"));
    }
}
