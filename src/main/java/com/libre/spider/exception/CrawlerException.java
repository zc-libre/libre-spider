package com.libre.spider.exception;

/**
 * 爬虫异常基类
 */
public class CrawlerException extends RuntimeException {

	public CrawlerException(String message) {
		super(message);
	}

	public CrawlerException(String message, Throwable cause) {
		super(message, cause);
	}

}