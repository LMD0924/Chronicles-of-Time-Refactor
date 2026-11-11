package com.chroniclesoftime.shared;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.lang.reflect.Method;
import java.util.List;

/** 基于 MyBatis-Plus BaseMapper 的通用仓储实现（全工程共享一个 DataSource）。 */
public abstract class BaseMapperRepository<T, M extends BaseMapper<T>> implements Repository<T, Long> {
    protected final M mapper;

    protected BaseMapperRepository(M mapper) { this.mapper = mapper; }

    @Override public T findById(Long id) { return mapper.selectById(id); }
    @Override public List<T> findAll() { return mapper.selectList(null); }
    @Override public List<T> findBy(String column, Object value) {
        return mapper.selectList(new QueryWrapper<T>().eq(column, value));
    }
    @Override public T save(T entity) {
        Object id = readId(entity);
        if (id == null) mapper.insert(entity); else mapper.updateById(entity);
        return entity;
    }
    @Override public boolean deleteById(Long id) { return mapper.deleteById(id) > 0; }
    @Override public long count() { return mapper.selectCount(null); }

    protected M mapper() { return mapper; }

    private Object readId(T entity) {
        try {
            Method m = entity.getClass().getMethod("getId");
            return m.invoke(entity);
        } catch (ReflectiveOperationException e) { return null; }
    }
}
