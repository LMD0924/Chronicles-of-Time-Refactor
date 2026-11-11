package com.chroniclesoftime.shared;

import java.util.List;

/** DDD 仓储契约：由 domain 层定义，infrastructure 层实现。 */
public interface Repository<T, ID> {
    T findById(ID id);
    List<T> findAll();
    List<T> findBy(String column, Object value);
    T save(T entity);
    boolean deleteById(ID id);
    long count();
}
