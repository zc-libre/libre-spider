package com.libre.spider.exception;

/**
 * API限流异常
 */
public class RateLimitException extends CrawlerException {

	public RateLimitException(String message) {
		super(message);
	}

	public RateLimitException(String message, Throwable cause) {
		super(message, cause);
	}

}