package com.libre.spider.service;

import com.libre.spider.entity.CrawlTask;
import com.libre.spider.mapper.CrawlTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 任务数据访问服务 继承基础数据服务类，提供任务相关的数据操作
 */
@Slf4j
@Service
public class TaskDataService extends BaseDataService<CrawlTask, CrawlTaskMapper> {

	@Override
	protected String getEntityName() {
		return "爬取任务";
	}

	@Override
	protected Object getId(CrawlTask entity) {
		return entity.getId();
	}

	@Override
	protected void setId(CrawlTask entity, Object id) {
		entity.setId((Long) id);
	}

	@Override
	protected void setCreateTime(CrawlTask entity) {
		entity.setCreateTime(LocalDateTime.now());
	}

	@Override
	protected void setUpdateTime(CrawlTask entity) {
		entity.setUpdateTime(LocalDateTime.now());
	}

	/**
	 * 创建新的爬取任务
	 * @param taskName 任务名称
	 * @param keyword 关键词
	 * @return 任务ID
	 */
	public Long createTask(String taskName, String keyword) {
		CrawlTask task = new CrawlTask();
		task.setTaskName(taskName);
		task.setKeyword(keyword);
		task.setStatus("PENDING");

		int insertCount = mapper.insert(task);
		if (insertCount > 0) {
			log.info("创建爬取任务成功: id={}, taskName={}, keyword={}", task.getId(), taskName, keyword);
			return task.getId();
		}
		else {
			log.error("创建爬取任务失败: taskName={}, keyword={}", taskName, keyword);
			throw new RuntimeException("创建爬取任务失败");
		}
	}

	/**
	 * 启动任务
	 * @param taskId 任务ID
	 */
	public void startTask(Long taskId) {
		CrawlTask task = new CrawlTask();
		task.setId(taskId);
		task.setStatus("RUNNING");
		task.setStartTime(LocalDateTime.now());
		task.setUpdateTime(LocalDateTime.now());

		int updateCount = mapper.updateById(task);
		if (updateCount > 0) {
			log.info("启动爬取任务成功: taskId={}", taskId);
		}
		else {
			log.warn("启动爬取任务失败: taskId={}", taskId);
		}
	}

	/**
	 * 完成任务
	 * @param taskId 任务ID
	 * @param totalNotes 总笔记数
	 * @param successNotes 成功笔记数
	 * @param failedNotes 失败笔记数
	 * @param errorMessage 错误信息
	 */
	public void completeTask(Long taskId, int totalNotes, int successNotes, int failedNotes, String errorMessage) {
		CrawlTask task = new CrawlTask();
		task.setId(taskId);
		task.setStatus(failedNotes > 0 ? "FAILED" : "SUCCESS");
		task.setEndTime(LocalDateTime.now());
		task.setTotalNotes(totalNotes);
		task.setSuccessNotes(successNotes);
		task.setFailedNotes(failedNotes);
		task.setErrorMessage(errorMessage);
		task.setUpdateTime(LocalDateTime.now());

		int updateCount = mapper.updateById(task);
		if (updateCount > 0) {
			log.info("完成爬取任务成功: taskId={}, total={}, success={}, failed={}", taskId, totalNotes, successNotes,
					failedNotes);
		}
		else {
			log.warn("完成爬取任务失败: taskId={}", taskId);
		}
	}

}