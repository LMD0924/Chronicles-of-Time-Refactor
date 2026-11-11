package com.chroniclesoftime.learning.domain.repository;

import com.chroniclesoftime.learning.domain.model.Question;
import com.chroniclesoftime.shared.Repository;

/** 学习 · 仓储契约（domain 层定义）。 */
public interface QuestionRepository extends Repository<Question, Long> {}
