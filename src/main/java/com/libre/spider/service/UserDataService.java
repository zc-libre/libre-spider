package com.libre.spider.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.libre.spider.entity.UserInfo;
import com.libre.spider.mapper.UserInfoMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户数据访问服务 继承基础数据服务类，提供用户相关的数据操作
 */
@Service
public class UserDataService extends BaseDataService<UserInfo, UserInfoMapper> {

	@Override
	protected String getEntityName() {
		return "用户";
	}

	@Override
	protected Object getId(UserInfo entity) {
		return entity.getId();
	}

	@Override
	protected void setId(UserInfo entity, Object id) {
		entity.setId((Long) id);
	}

	@Override
	protected void setCreateTime(UserInfo entity) {
		entity.setCreateTime(LocalDateTime.now());
	}

	@Override
	protected void setUpdateTime(UserInfo entity) {
		entity.setUpdateTime(LocalDateTime.now());
	}

	/**
	 * 根据用户ID查询用户信息
	 * @param userId 用户ID
	 * @return 用户信息
	 */
	public UserInfo findByUserId(String userId) {
		LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(UserInfo::getUserId, userId);
		return findOne(wrapper);
	}

	/**
	 * 检查用户是否已存在
	 * @param userId 用户ID
	 * @return 是否存在
	 */
	public boolean existsByUserId(String userId) {
		return findByUserId(userId) != null;
	}

	/**
	 * 保存或更新用户信息（根据userId）
	 * @param userInfo 用户信息
	 */
	public void saveOrUpdateByUserId(UserInfo userInfo) {
		LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(UserInfo::getUserId, userInfo.getUserId());
		saveOrUpdate(userInfo, wrapper, null);
	}

}