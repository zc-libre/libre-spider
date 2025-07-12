package com.libre.spider.exception;

/**
 * IP被封禁异常
 */
public class IpBlockedException extends CrawlerException {

	public IpBlockedException(String message) {
		super(message);
	}

	public IpBlockedException(String message, Throwable cause) {
		super(message, cause);
	}

}