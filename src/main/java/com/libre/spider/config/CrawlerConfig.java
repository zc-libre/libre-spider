package com.libre.spider.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 爬虫配置类 用于管理爬虫的各种配置参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "xhs.crawler")
public class CrawlerConfig {

	/**
	 * 关键词配置（支持多个关键词，用逗号分隔）
	 */
	private String keywords = "";

	/**
	 * 每个关键词最大抓取笔记数量
	 */
	private int maxNotesPerKeyword = 20;

	/**
	 * 搜索页数（每页通常10-20条）
	 */
	private int maxPages = 2;

	/**
	 * 排序类型：GENERAL(综合), TIME_DESCENDING(最新), POPULARITY_DESCENDING(最热)
	 */
	private String sortType = "GENERAL";

	/**
	 * 是否获取笔记详情
	 */
	private boolean enableNoteDetail = true;

	/**
	 * 是否获取用户信息
	 */
	private boolean enableUserInfo = true;

	/**
	 * 是否获取评论信息
	 */
	private boolean enableComments = false;

	/**
	 * 请求间隔（毫秒）
	 */
	private long requestInterval = 3000;

	/**
	 * 并发数量（批量处理时）
	 */
	private int concurrency = 3;

	/**
	 * 重试次数
	 */
	private int retryCount = 3;

	/**
	 * 数据存储配置
	 */
	private StorageConfig storage = new StorageConfig();

	/**
	 * 定时任务配置
	 */
	private ScheduleConfig schedule = new ScheduleConfig();

	/**
	 * 获取关键词列表
	 */
	public List<String> getKeywordList() {
		return List.of(keywords.split(","));
	}

	/**
	 * 数据存储配置
	 */
	@Data
	public static class StorageConfig {

		/**
		 * 存储类型：DATABASE(数据库), JSON(JSON文件), CSV(CSV文件), ALL(所有)
		 */
		private String type = "DATABASE";

		/**
		 * 文件存储路径
		 */
		private String filePath = "data/xhs";

		/**
		 * 是否去重（基于noteId）
		 */
		private boolean enableDeduplication = true;

	}

	/**
	 * 定时任务配置
	 */
	@Data
	public static class ScheduleConfig {

		/**
		 * 是否启用定时任务
		 */
		private boolean enabled = true;

		/**
		 * Cron表达式（默认每天8点执行）
		 */
		private String cron = "0 0 8 * * ?";

		/**
		 * 任务超时时间（分钟）
		 */
		private int timeout = 60;

	}

}