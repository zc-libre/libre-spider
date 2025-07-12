package com.libre.spider.service;

import com.libre.spider.config.CrawlerConfig;
import com.libre.spider.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 定时任务爬虫服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "xhs.crawler.schedule.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledCrawlerService {

	private final CrawlerConfig crawlerConfig;

	private final BatchCrawlerService batchCrawlerService;

	/**
	 * 定时执行爬虫任务 默认每天8点执行，可通过配置文件修改
	 */
	@Scheduled(cron = "#{@crawlerConfig.schedule.cron}")
	public void scheduledCrawl() {
		log.info("=== 开始执行定时爬虫任务 ===");

		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		log.info("执行时间: {}", timestamp);

		try {
			// 获取配置的关键词列表
			List<String> keywords = crawlerConfig.getKeywordList();
			log.info("配置的关键词: {}", keywords);

			// 为每个关键词创建爬取任务
			for (String keyword : keywords) {
				String taskName = String.format("定时任务-%s-%s", keyword,
						LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")));

				log.info("开始处理关键词: {}", keyword);

				// 执行批量爬取
				batchCrawlerService.executeBatchCrawl(taskName, keyword);

				log.info("完成处理关键词: {}", keyword);

				// 关键词之间的间隔
				if (keywords.size() > 1) {
					try {
						Thread.sleep(crawlerConfig.getRequestInterval());
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						log.warn("定时任务被中断");
						break;
					}
				}
			}

			log.info("=== 定时爬虫任务执行完成 ===");

		}
		catch (Exception e) {
			handleScheduledTaskException(e);
		}
	}

	/**
	 * 手动触发爬虫任务
	 */
	public void manualCrawl() {
		log.info("=== 手动触发爬虫任务 ===");
		scheduledCrawl();
	}

	/**
	 * 为指定关键词手动触发爬虫任务
	 */
	public void manualCrawlForKeyword(String keyword) {
		log.info("=== 手动触发关键词爬虫任务: {} ===", keyword);

		String taskName = String.format("手动任务-%s-%s", keyword,
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")));

		try {
			batchCrawlerService.executeBatchCrawl(taskName, keyword);
			log.info("=== 手动关键词爬虫任务完成: {} ===", keyword);
		}
		catch (Exception e) {
			handleTaskException(e, "手动关键词爬虫任务失败: " + keyword);
		}
	}

	/**
	 * 处理定时任务异常
	 */
	private void handleScheduledTaskException(Exception e) {
		if (e instanceof IpBlockedException) {
			log.error("定时任务失败 - IP被封禁，建议更换IP或稍后重试: {}", e.getMessage());
		}
		else if (e instanceof RateLimitException) {
			log.warn("定时任务失败 - API限流，系统将自动重试: {}", e.getMessage());
		}
		else if (e instanceof LoginRequiredException) {
			log.error("定时任务失败 - 需要重新登录，请检查Cookie配置: {}", e.getMessage());
		}
		else if (e instanceof DataFetchException) {
			log.warn("定时任务失败 - 数据获取异常，系统已重试: {}", e.getMessage());
		}
		else {
			log.error("定时任务执行失败", e);
		}
	}

	/**
	 * 处理任务异常
	 */
	private void handleTaskException(Exception e, String context) {
		if (e instanceof IpBlockedException) {
			log.error("{} - IP被封禁: {}", context, e.getMessage());
		}
		else if (e instanceof RateLimitException) {
			log.warn("{} - API限流: {}", context, e.getMessage());
		}
		else if (e instanceof LoginRequiredException) {
			log.error("{} - 需要重新登录: {}", context, e.getMessage());
		}
		else if (e instanceof DataFetchException) {
			log.warn("{} - 数据获取异常: {}", context, e.getMessage());
		}
		else {
			log.error(context, e);
		}
	}

}