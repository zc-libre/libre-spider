package com.libre.spider.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务状态实体
 */
@Data
@TableName("crawler_task_status")
public class TaskStatusEntity {

	@TableId(type = IdType.AUTO)
	private Long id;

	/**
	 * 任务名称
	 */
	private String taskName;

	/**
	 * 关键词
	 */
	private String keyword;

	/**
	 * 任务状态：RUNNING, COMPLETED, FAILED
	 */
	private String status;

	/**
	 * 开始时间
	 */
	private LocalDateTime startTime;

	/**
	 * 结束时间
	 */
	private LocalDateTime endTime;

	/**
	 * 爬取的笔记数量
	 */
	private Integer noteCount;

	/**
	 * 爬取的用户数量
	 */
	private Integer userCount;

	/**
	 * 错误数量
	 */
	private Integer errorCount;

	/**
	 * 错误信息
	 */
	@TableField(value = "error_message")
	private String errorMessage;

	/**
	 * 创建时间
	 */
	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createTime;

	/**
	 * 更新时间
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updateTime;

}