package com.libre.spider.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.libre.spider.entity.NoteInfo;
import com.libre.spider.mapper.NoteInfoMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 笔记数据访问服务 继承基础数据服务类，提供笔记相关的数据操作
 */
@Service
public class NoteDataService extends BaseDataService<NoteInfo, NoteInfoMapper> {

	@Override
	protected String getEntityName() {
		return "笔记";
	}

	@Override
	protected Object getId(NoteInfo entity) {
		return entity.getId();
	}

	@Override
	protected void setId(NoteInfo entity, Object id) {
		entity.setId((Long) id);
	}

	@Override
	protected void setCreateTime(NoteInfo entity) {
		entity.setCreateTime(LocalDateTime.now());
	}

	@Override
	protected void setUpdateTime(NoteInfo entity) {
		entity.setUpdateTime(LocalDateTime.now());
	}

	/**
	 * 根据笔记ID查询笔记信息
	 * @param noteId 笔记ID
	 * @return 笔记信息
	 */
	public NoteInfo findByNoteId(String noteId) {
		LambdaQueryWrapper<NoteInfo> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(NoteInfo::getNoteId, noteId);
		return findOne(wrapper);
	}

	/**
	 * 检查笔记是否已存在
	 * @param noteId 笔记ID
	 * @return 是否存在
	 */
	public boolean existsByNoteId(String noteId) {
		return findByNoteId(noteId) != null;
	}

	/**
	 * 保存或更新笔记信息（根据noteId）
	 * @param noteInfo 笔记信息
	 */
	public void saveOrUpdateByNoteId(NoteInfo noteInfo) {
		LambdaQueryWrapper<NoteInfo> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(NoteInfo::getNoteId, noteInfo.getNoteId());
		saveOrUpdate(noteInfo, wrapper, null);
	}

}