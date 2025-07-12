package com.libre.spider.exception;

/**
 * 小红书爬虫异常类
 */
public class XhsException extends RuntimeException {

	private final int code;

	public XhsException(String message) {
		super(message);
		this.code = -1;
	}

	public XhsException(String message, int code) {
		super(message);
		this.code = code;
	}

	public XhsException(String message, Throwable cause) {
		super(message, cause);
		this.code = -1;
	}

	public XhsException(String message, int code, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	/**
	 * 数据获取异常
	 */
	public static class DataFetchError extends XhsException {

		public DataFetchError(String message) {
			super(message, 1001);
		}

	}

	/**
	 * IP被封禁异常
	 */
	public static class IPBlockError extends XhsException {

		public IPBlockError(String message) {
			super(message, 1002);
		}

	}

	/**
	 * 验证码异常
	 */
	public static class VerificationError extends XhsException {

		public VerificationError(String message) {
			super(message, 1003);
		}

	}

	/**
	 * 参数解析异常
	 */
	public static class ParameterError extends XhsException {

		public ParameterError(String message) {
			super(message, 1004);
		}

	}

}