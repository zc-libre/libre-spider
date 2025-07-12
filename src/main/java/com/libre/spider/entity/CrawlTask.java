package com.libre.spider.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 爬取任务实体类
 */
@Data
@TableName("crawl_task")
public class CrawlTask {

	@TableId(type = IdType.AUTO)
	private Long id;

	/**
	 * 任务名称
	 */
	@TableField("task_name")
	private String taskName;

	/**
	 * 关键词
	 */
	@TableField("keyword")
	private String keyword;

	/**
	 * 任务状态：PENDING(待执行), RUNNING(运行中), SUCCESS(成功), FAILED(失败)
	 */
	@TableField("status")
	private String status;

	/**
	 * 开始时间
	 */
	@TableField("start_time")
	private LocalDateTime startTime;

	/**
	 * 结束时间
	 */
	@TableField("end_time")
	private LocalDateTime endTime;

	/**
	 * 总笔记数
	 */
	@TableField("total_notes")
	private Integer totalNotes;

	/**
	 * 成功笔记数
	 */
	@TableField("success_notes")
	private Integer successNotes;

	/**
	 * 失败笔记数
	 */
	@TableField("failed_notes")
	private Integer failedNotes;

	/**
	 * 错误信息
	 */
	@TableField("error_message")
	private String errorMessage;

	/**
	 * 创建时间
	 */
	@TableField("create_time")
	private LocalDateTime createTime;

	/**
	 * 更新时间
	 */
	@TableField("update_time")
	private LocalDateTime updateTime;

}