package com.libre.spider.config;

import com.libre.spider.exception.DataFetchException;
import com.libre.spider.exception.IpBlockedException;
import com.libre.spider.exception.RateLimitException;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * 重试机制配置
 */
@Configuration
@EnableRetry
public class RetryConfig {

	/**
	 * 重试模板配置
	 */
	@Component
	public static class RetryTemplate {

		/**
		 * 数据获取重试 - 针对网络异常和数据获取失败
		 */
		@Retryable(retryFor = { DataFetchException.class, RuntimeException.class }, maxAttempts = 3,
				backoff = @Backoff(delay = 1000, multiplier = 2))
		public <T> T executeWithRetry(java.util.function.Supplier<T> operation) {
			return operation.get();
		}

		/**
		 * API限流重试 - 使用更长的退避时间
		 */
		@Retryable(retryFor = { RateLimitException.class }, maxAttempts = 5,
				backoff = @Backoff(delay = 5000, multiplier = 1.5))
		public <T> T executeWithRateLimitRetry(java.util.function.Supplier<T> operation) {
			return operation.get();
		}

		/**
		 * IP封禁不重试，直接抛出异常
		 */
		public <T> T executeWithoutRetryForIpBlock(java.util.function.Supplier<T> operation) throws IpBlockedException {
			return operation.get();
		}

	}

}