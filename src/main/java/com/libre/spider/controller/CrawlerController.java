package com.libre.spider.controller;

import com.libre.spider.config.CrawlerConfig;
import com.libre.spider.service.ScheduledCrawlerService;
import com.libre.spider.service.CrawlerStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 爬虫控制器 提供手动触发爬虫任务的接口
 */
@Slf4j
@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
public class CrawlerController {

	private final ScheduledCrawlerService scheduledCrawlerService;

	private final CrawlerConfig crawlerConfig;

	private final CrawlerStatusService crawlerStatusService;

	/**
	 * 手动触发全量爬虫任务
	 */
	@PostMapping("/trigger")
	public Map<String, Object> triggerCrawler() {
		log.info("收到手动触发爬虫任务请求");

		Map<String, Object> result = new HashMap<>();

		try {
			scheduledCrawlerService.manualCrawl();

			result.put("success", true);
			result.put("message", "爬虫任务已启动");
			result.put("keywords", crawlerConfig.getKeywordList());

		}
		catch (Exception e) {
			log.error("手动触发爬虫任务失败", e);
			result.put("success", false);
			result.put("message", "爬虫任务启动失败: " + e.getMessage());
		}

		return result;
	}

	/**
	 * 手动触发指定关键词的爬虫任务
	 */
	@PostMapping("/trigger/{keyword}")
	public Map<String, Object> triggerCrawlerForKeyword(@PathVariable String keyword) {
		log.info("收到手动触发关键词爬虫任务请求: {}", keyword);

		Map<String, Object> result = new HashMap<>();

		try {
			scheduledCrawlerService.manualCrawlForKeyword(keyword);

			result.put("success", true);
			result.put("message", "关键词爬虫任务已启动");
			result.put("keyword", keyword);

		}
		catch (Exception e) {
			log.error("手动触发关键词爬虫任务失败: {}", keyword, e);
			result.put("success", false);
			result.put("message", "关键词爬虫任务启动失败: " + e.getMessage());
		}

		return result;
	}

	/**
	 * 获取爬虫配置信息
	 */
	@GetMapping("/config")
	public Map<String, Object> getCrawlerConfig() {
		Map<String, Object> config = new HashMap<>();

		config.put("keywords", crawlerConfig.getKeywordList());
		config.put("maxNotesPerKeyword", crawlerConfig.getMaxNotesPerKeyword());
		config.put("maxPages", crawlerConfig.getMaxPages());
		config.put("sortType", crawlerConfig.getSortType());
		config.put("enableNoteDetail", crawlerConfig.isEnableNoteDetail());
		config.put("enableUserInfo", crawlerConfig.isEnableUserInfo());
		config.put("enableComments", crawlerConfig.isEnableComments());
		config.put("requestInterval", crawlerConfig.getRequestInterval());
		config.put("concurrency", crawlerConfig.getConcurrency());
		config.put("retryCount", crawlerConfig.getRetryCount());
		config.put("scheduleEnabled", crawlerConfig.getSchedule().isEnabled());
		config.put("scheduleCron", crawlerConfig.getSchedule().getCron());
		config.put("storageType", crawlerConfig.getStorage().getType());
		config.put("enableDeduplication", crawlerConfig.getStorage().isEnableDeduplication());

		return config;
	}

	/**
	 * 更新关键词配置
	 */
	@PostMapping("/config/keywords")
	public Map<String, Object> updateKeywords(@RequestBody Map<String, Object> request) {
		Map<String, Object> result = new HashMap<>();

		try {
			@SuppressWarnings("unchecked")
			List<String> keywords = (List<String>) request.get("keywords");

			if (keywords != null && !keywords.isEmpty()) {
				String keywordString = String.join(",", keywords);
				crawlerConfig.setKeywords(keywordString);

				result.put("success", true);
				result.put("message", "关键词配置已更新");
				result.put("keywords", crawlerConfig.getKeywordList());
			}
			else {
				result.put("success", false);
				result.put("message", "关键词列表不能为空");
			}

		}
		catch (Exception e) {
			log.error("更新关键词配置失败", e);
			result.put("success", false);
			result.put("message", "更新关键词配置失败: " + e.getMessage());
		}

		return result;
	}

	/**
	 * 获取爬虫状态
	 */
	@GetMapping("/status")
	public Map<String, Object> getCrawlerStatus() {
		Map<String, Object> status = new HashMap<>();

		status.put("scheduleEnabled", crawlerConfig.getSchedule().isEnabled());
		status.put("scheduleCron", crawlerConfig.getSchedule().getCron());
		status.put("keywords", crawlerConfig.getKeywordList());
		status.put("timestamp", System.currentTimeMillis());

		// 添加监控统计信息
		status.put("stats", crawlerStatusService.getGlobalStats());

		return status;
	}

	/**
	 * 获取所有任务状态
	 */
	@GetMapping("/tasks")
	public Map<String, Object> getAllTaskStatus() {
		Map<String, Object> result = new HashMap<>();

		result.put("tasks", crawlerStatusService.getAllTaskStatus());
		result.put("globalStats", crawlerStatusService.getGlobalStats());

		return result;
	}

	/**
	 * 获取指定任务状态
	 */
	@GetMapping("/task/{taskName}")
	public Map<String, Object> getTaskStatus(@PathVariable String taskName) {
		Map<String, Object> result = new HashMap<>();

		CrawlerStatusService.TaskStatus taskStatus = crawlerStatusService.getTaskStatus(taskName);

		if (taskStatus != null) {
			result.put("success", true);
			result.put("taskStatus", taskStatus);
		}
		else {
			result.put("success", false);
			result.put("message", "任务不存在");
		}

		return result;
	}

}