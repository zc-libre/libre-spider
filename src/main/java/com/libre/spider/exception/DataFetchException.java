package com.libre.spider.exception;

/**
 * 数据获取异常
 */
public class DataFetchException extends CrawlerException {

	public DataFetchException(String message) {
		super(message);
	}

	public DataFetchException(String message, Throwable cause) {
		super(message, cause);
	}

}