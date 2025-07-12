package com.libre.spider.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.libre.spider.entity.TaskStatusEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务状态Mapper
 */
@Mapper
public interface TaskStatusMapper extends BaseMapper<TaskStatusEntity> {

	/**
	 * 查询指定时间之前的任务
	 */
	@Select("SELECT * FROM crawler_task_status WHERE end_time < #{beforeTime} ORDER BY end_time DESC")
	List<TaskStatusEntity> selectTasksBefore(LocalDateTime beforeTime);

	/**
	 * 查询运行中的任务数量
	 */
	@Select("SELECT COUNT(*) FROM crawler_task_status WHERE status = 'RUNNING'")
	int countRunningTasks();

	/**
	 * 查询最近的任务状态
	 */
	@Select("SELECT * FROM crawler_task_status ORDER BY create_time DESC LIMIT #{limit}")
	List<TaskStatusEntity> selectRecentTasks(int limit);

}