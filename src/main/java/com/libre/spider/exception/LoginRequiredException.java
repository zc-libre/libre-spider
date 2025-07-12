package com.libre.spider.exception;

/**
 * 需要登录异常
 */
public class LoginRequiredException extends CrawlerException {

	public LoginRequiredException(String message) {
		super(message);
	}

	public LoginRequiredException(String message, Throwable cause) {
		super(message, cause);
	}

}