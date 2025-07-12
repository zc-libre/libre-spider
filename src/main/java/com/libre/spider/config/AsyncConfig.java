package com.libre.spider.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

	/**
	 * 爬虫任务线程池
	 */
	@Bean(name = "crawlerTaskExecutor")
	public Executor crawlerTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		// 核心线程数
		executor.setCorePoolSize(2);

		// 最大线程数
		executor.setMaxPoolSize(4);

		// 队列容量
		executor.setQueueCapacity(100);

		// 线程名称前缀
		executor.setThreadNamePrefix("crawler-");

		// 拒绝策略
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

		// 线程空闲时间
		executor.setKeepAliveSeconds(60);

		// 等待任务完成后再关闭线程池
		executor.setWaitForTasksToCompleteOnShutdown(true);

		// 等待时间
		executor.setAwaitTerminationSeconds(60);

		executor.initialize();

		log.info("初始化爬虫任务线程池完成");
		return executor;
	}

}