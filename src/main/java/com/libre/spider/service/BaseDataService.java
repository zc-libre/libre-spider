package com.libre.spider.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.libre.spider.utils.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * 数据访问层基础服务类 提供通用的数据库操作方法，避免重复的保存/更新逻辑
 *
 * @param <T> 实体类型
 * @param <M> Mapper类型
 */
@Slf4j
public abstract class BaseDataService<T, M extends BaseMapper<T>> {

	@Autowired
	protected M mapper;

	@Autowired
	protected ExceptionHandler exceptionHandler;

	/**
	 * 保存或更新实体 如果实体已存在则更新，否则插入新记录
	 * @param entity 实体对象
	 * @param uniqueCondition 唯一性查询条件
	 * @param updateCallback 更新时的回调函数
	 */
	@Transactional
	public void saveOrUpdate(T entity, LambdaQueryWrapper<T> uniqueCondition, Consumer<T> updateCallback) {
		try {
			T existingEntity = mapper.selectOne(uniqueCondition);

			if (existingEntity != null) {
				// 更新现有记录
				log.debug("更新{}记录", getEntityName());

				// 设置更新时间
				setUpdateTime(entity);

				// 执行更新回调
				if (updateCallback != null) {
					updateCallback.accept(existingEntity);
				}

				// 复制ID
				setId(entity, getId(existingEntity));

				int updateCount = mapper.updateById(entity);
				if (updateCount > 0) {
					log.debug("{}记录更新成功", getEntityName());
				}
				else {
					log.warn("{}记录更新失败，可能已被删除", getEntityName());
				}
			}
			else {
				// 插入新记录
				log.debug("插入新{}记录", getEntityName());

				// 设置创建和更新时间
				setCreateTime(entity);
				setUpdateTime(entity);

				int insertCount = mapper.insert(entity);
				if (insertCount > 0) {
					log.debug("{}记录插入成功", getEntityName());
				}
				else {
					log.error("{}记录插入失败", getEntityName());
				}
			}
		}
		catch (Exception e) {
			exceptionHandler.handleDatabaseException(e, "保存或更新", getEntityName());
			throw e;
		}
	}

	/**
	 * 批量保存或更新实体
	 * @param entities 实体列表
	 * @param uniqueConditionProvider 唯一性查询条件提供者
	 */
	@Transactional
	public void batchSaveOrUpdate(List<T> entities,
			java.util.function.Function<T, LambdaQueryWrapper<T>> uniqueConditionProvider) {
		if (entities == null || entities.isEmpty()) {
			return;
		}

		log.info("开始批量保存或更新{}条{}记录", entities.size(), getEntityName());

		int successCount = 0;
		int failCount = 0;

		for (T entity : entities) {
			try {
				LambdaQueryWrapper<T> condition = uniqueConditionProvider.apply(entity);
				saveOrUpdate(entity, condition, null);
				successCount++;
			}
			catch (Exception e) {
				failCount++;
				log.error("批量保存{}记录失败", getEntityName(), e);
			}
		}

		log.info("批量保存{}记录完成: 成功{}条, 失败{}条", getEntityName(), successCount, failCount);
	}

	/**
	 * 根据条件查询单个实体
	 * @param condition 查询条件
	 * @return 实体对象，不存在时返回null
	 */
	public T findOne(LambdaQueryWrapper<T> condition) {
		try {
			return mapper.selectOne(condition);
		}
		catch (Exception e) {
			exceptionHandler.handleDatabaseException(e, "查询", getEntityName());
			throw e;
		}
	}

	/**
	 * 根据条件查询实体列表
	 * @param condition 查询条件
	 * @return 实体列表
	 */
	public List<T> findList(LambdaQueryWrapper<T> condition) {
		try {
			return mapper.selectList(condition);
		}
		catch (Exception e) {
			exceptionHandler.handleDatabaseException(e, "查询列表", getEntityName());
			throw e;
		}
	}

	/**
	 * 根据ID删除实体
	 * @param id 实体ID
	 * @return 是否删除成功
	 */
	public boolean deleteById(Object id) {
		try {
			int deleteCount = mapper.deleteById((java.io.Serializable) id);
			boolean success = deleteCount > 0;

			if (success) {
				log.debug("删除{}记录成功: id={}", getEntityName(), id);
			}
			else {
				log.warn("删除{}记录失败: id={}", getEntityName(), id);
			}

			return success;
		}
		catch (Exception e) {
			exceptionHandler.handleDatabaseException(e, "删除", getEntityName());
			throw e;
		}
	}

	/**
	 * 获取实体名称（用于日志输出） 子类必须实现此方法
	 * @return 实体名称
	 */
	protected abstract String getEntityName();

	/**
	 * 获取实体ID 子类可以重写此方法
	 * @param entity 实体对象
	 * @return 实体ID
	 */
	protected abstract Object getId(T entity);

	/**
	 * 设置实体ID 子类可以重写此方法
	 * @param entity 实体对象
	 * @param id ID值
	 */
	protected abstract void setId(T entity, Object id);

	/**
	 * 设置创建时间 子类可以重写此方法
	 * @param entity 实体对象
	 */
	protected void setCreateTime(T entity) {
		// 默认实现为空，子类可根据需要重写
	}

	/**
	 * 设置更新时间 子类可以重写此方法
	 * @param entity 实体对象
	 */
	protected void setUpdateTime(T entity) {
		// 默认实现为空，子类可根据需要重写
	}

}