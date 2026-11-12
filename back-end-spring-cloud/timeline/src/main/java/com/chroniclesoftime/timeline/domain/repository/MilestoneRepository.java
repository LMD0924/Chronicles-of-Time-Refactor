package com.chroniclesoftime.timeline.domain.repository;

import com.chroniclesoftime.timeline.domain.model.Milestone;
import com.chroniclesoftime.shared.Repository;

/** 拾光时间线 · 仓储契约（domain 层定义）。 */
public interface MilestoneRepository extends Repository<Milestone, Long> {
    /**
     * 显式把 linked_record_id 置空并标记为 MANUAL。
     * MyBatis-Plus 的 updateById 默认忽略 null 字段，必须用 UpdateWrapper 才能清空外键。
     */
    void clearLinkedRecord(Long id);
}
