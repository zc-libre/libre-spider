package com.libre.spider.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.libre.spider.entity.TaskStatusEntity;
import com.libre.spider.mapper.TaskStatusMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 爬虫状态监控服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.datasource.url", matchIfMissing = false)
public class CrawlerStatusService {

	private final TaskStatusMapper taskStatusMapper;

	/**
	 * 任务状态信息
	 */
	private final Map<String, TaskStatus> taskStatusMap = new ConcurrentHashMap<>();

	/**
	 * 全局统计信息
	 */
	private final AtomicLong totalTaskCount = new AtomicLong(0);

	private final AtomicLong totalNoteCount = new AtomicLong(0);

	private final AtomicLong totalUserCount = new AtomicLong(0);

	private final AtomicLong totalErrorCount = new AtomicLong(0);

	private final AtomicInteger currentRunningTasks = new AtomicInteger(0);

	/**
	 * 记录任务开始
	 */
	public void recordTaskStart(String taskName, String keyword) {
		TaskStatus status = new TaskStatus();
		status.setTaskName(taskName);
		status.setKeyword(keyword);
		status.setStatus("RUNNING");
		status.setStartTime(LocalDateTime.now());

		taskStatusMap.put(taskName, status);
		currentRunningTasks.incrementAndGet();
		totalTaskCount.incrementAndGet();

		// 持久化到数据库
		try {
			TaskStatusEntity entity = new TaskStatusEntity();
			entity.setTaskName(taskName);
			entity.setKeyword(keyword);
			entity.setStatus("RUNNING");
			entity.setStartTime(LocalDateTime.now());
			entity.setNoteCount(0);
			entity.setUserCount(0);
			entity.setErrorCount(0);
			taskStatusMapper.insert(entity);
		}
		catch (Exception e) {
			log.warn("保存任务状态到数据库失败: {}", e.getMessage());
		}

		log.info("任务监控 - 任务开始: {}, 关键词: {}, 当前运行任务数: {}", taskName, keyword, currentRunningTasks.get());
	}

	/**
	 * 记录任务完成
	 */
	public void recordTaskComplete(String taskName, int noteCount, int userCount, int errorCount) {
		TaskStatus status = taskStatusMap.get(taskName);
		if (status != null) {
			status.setStatus("COMPLETED");
			status.setEndTime(LocalDateTime.now());
			status.setNoteCount(noteCount);
			status.setUserCount(userCount);
			status.setErrorCount(errorCount);

			// 更新全局统计
			totalNoteCount.addAndGet(noteCount);
			totalUserCount.addAndGet(userCount);
			totalErrorCount.addAndGet(errorCount);
			currentRunningTasks.decrementAndGet();

			// 更新数据库记录
			try {
				TaskStatusEntity updateEntity = new TaskStatusEntity();
				updateEntity.setStatus("COMPLETED");
				updateEntity.setEndTime(LocalDateTime.now());
				updateEntity.setNoteCount(noteCount);
				updateEntity.setUserCount(userCount);
				updateEntity.setErrorCount(errorCount);

				QueryWrapper<TaskStatusEntity> wrapper = new QueryWrapper<>();
				wrapper.eq("task_name", taskName).eq("status", "RUNNING");
				taskStatusMapper.update(updateEntity, wrapper);
			}
			catch (Exception e) {
				log.warn("更新任务状态到数据库失败: {}", e.getMessage());
			}

			log.info("任务监控 - 任务完成: {}, 笔记数: {}, 用户数: {}, 错误数: {}, 耗时: {}ms", taskName, noteCount, userCount, errorCount,
					java.time.Duration.between(status.getStartTime(), status.getEndTime()).toMillis());
		}
	}

	/**
	 * 记录任务失败
	 */
	public void recordTaskFailure(String taskName, String errorMessage) {
		TaskStatus status = taskStatusMap.get(taskName);
		if (status != null) {
			status.setStatus("FAILED");
			status.setEndTime(LocalDateTime.now());
			status.setErrorMessage(errorMessage);

			currentRunningTasks.decrementAndGet();
			totalErrorCount.incrementAndGet();

			// 更新数据库记录
			try {
				TaskStatusEntity updateEntity = new TaskStatusEntity();
				updateEntity.setStatus("FAILED");
				updateEntity.setEndTime(LocalDateTime.now());
				updateEntity.setErrorMessage(errorMessage);

				QueryWrapper<TaskStatusEntity> wrapper = new QueryWrapper<>();
				wrapper.eq("task_name", taskName).eq("status", "RUNNING");
				taskStatusMapper.update(updateEntity, wrapper);
			}
			catch (Exception e) {
				log.warn("更新任务状态到数据库失败: {}", e.getMessage());
			}

			log.error("任务监控 - 任务失败: {}, 错误信息: {}", taskName, errorMessage);
		}
	}

	/**
	 * 记录请求统计
	 */
	public void recordRequest(String type, boolean success, long duration) {
		if (success) {
			log.debug("请求监控 - 请求成功: type={}, duration={}ms", type, duration);
		}
		else {
			log.warn("请求监控 - 请求失败: type={}, duration={}ms", type, duration);
		}
	}

	/**
	 * 获取任务状态
	 */
	public TaskStatus getTaskStatus(String taskName) {
		return taskStatusMap.get(taskName);
	}

	/**
	 * 获取全局统计信息
	 */
	public Map<String, Object> getGlobalStats() {
		return Map.of("totalTaskCount", totalTaskCount.get(), "totalNoteCount", totalNoteCount.get(), "totalUserCount",
				totalUserCount.get(), "totalErrorCount", totalErrorCount.get(), "currentRunningTasks",
				currentRunningTasks.get(), "timestamp", LocalDateTime.now());
	}

	/**
	 * 获取所有任务状态
	 */
	public Map<String, TaskStatus> getAllTaskStatus() {
		return Map.copyOf(taskStatusMap);
	}

	/**
	 * 从数据库获取最近的任务状态
	 */
	public List<TaskStatusEntity> getRecentTasksFromDb(int limit) {
		try {
			return taskStatusMapper.selectRecentTasks(limit);
		}
		catch (Exception e) {
			log.warn("从数据库获取任务状态失败: {}", e.getMessage());
			return List.of();
		}
	}

	/**
	 * 清理历史任务状态（保留最近100个）
	 */
	public void cleanupOldTasks() {
		if (taskStatusMap.size() > 100) {
			taskStatusMap.entrySet().removeIf(entry -> {
				TaskStatus status = entry.getValue();
				return status.getEndTime() != null && status.getEndTime().isBefore(LocalDateTime.now().minusHours(24));
			});
		}

		// 清理数据库中的老数据（保留最近30天）
		try {
			LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
			QueryWrapper<TaskStatusEntity> wrapper = new QueryWrapper<>();
			wrapper.lt("end_time", thirtyDaysAgo);
			int deleted = taskStatusMapper.delete(wrapper);
			if (deleted > 0) {
				log.info("清理了30天前的任务记录: {} 条", deleted);
			}
		}
		catch (Exception e) {
			log.warn("清理历史任务数据失败: {}", e.getMessage());
		}
	}

	/**
	 * 任务状态信息
	 */
	@Data
	public static class TaskStatus {

		private String taskName;

		private String keyword;

		private String status;

		private LocalDateTime startTime;

		private LocalDateTime endTime;

		private int noteCount;

		private int userCount;

		private int errorCount;

		private String errorMessage;

	}

}