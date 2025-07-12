package com.libre.spider.utils;

import com.libre.spider.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 异常处理工具类 统一处理爬虫系统中的各种异常情况
 */
@Slf4j
@Component
public class ExceptionHandler {

	/**
	 * 处理任务执行过程中的异常
	 * @param e 异常对象
	 * @param context 上下文信息
	 */
	public void handleTaskException(Exception e, String context) {
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
		else if (e instanceof CrawlerException) {
			log.error("{} - 爬虫执行异常: {}", context, e.getMessage());
		}
		else {
			log.error("{} - 未知异常", context, e);
		}
	}

	/**
	 * 处理HTTP请求异常
	 * @param e 异常对象
	 * @param url 请求URL
	 * @param method HTTP方法
	 */
	public void handleHttpException(Exception e, String url, String method) {
		String context = String.format("%s请求失败 [%s]", method, url);

		if (e instanceof IpBlockedException) {
			log.error("{} - IP被封禁，建议更换代理或稍后重试: {}", context, e.getMessage());
		}
		else if (e instanceof RateLimitException) {
			log.warn("{} - 请求频率过高，需要降低请求频率: {}", context, e.getMessage());
		}
		else if (e instanceof LoginRequiredException) {
			log.error("{} - 登录状态失效，需要重新获取Cookie: {}", context, e.getMessage());
		}
		else {
			log.error("{} - 网络请求异常", context, e);
		}
	}

	/**
	 * 处理数据解析异常
	 * @param e 异常对象
	 * @param dataType 数据类型描述
	 * @param source 数据来源
	 */
	public void handleParseException(Exception e, String dataType, String source) {
		String context = String.format("解析%s数据失败 [来源: %s]", dataType, source);
		log.error("{}: {}", context, e.getMessage());
		log.debug("解析异常详情", e);
	}

	/**
	 * 处理数据库操作异常
	 * @param e 异常对象
	 * @param operation 操作类型
	 * @param entityType 实体类型
	 */
	public void handleDatabaseException(Exception e, String operation, String entityType) {
		String context = String.format("数据库%s操作失败 [实体: %s]", operation, entityType);
		log.error("{}: {}", context, e.getMessage());
		log.debug("数据库异常详情", e);
	}

	/**
	 * 判断异常是否为可重试类型
	 * @param e 异常对象
	 * @return 是否可重试
	 */
	public boolean isRetryableException(Exception e) {
		return e instanceof RateLimitException
				|| (e instanceof DataFetchException && !(e instanceof LoginRequiredException))
				|| (e instanceof RuntimeException && e.getCause() instanceof java.net.SocketTimeoutException);
	}

	/**
	 * 判断异常是否需要立即停止任务
	 * @param e 异常对象
	 * @return 是否需要停止任务
	 */
	public boolean shouldStopTask(Exception e) {
		return e instanceof IpBlockedException || e instanceof LoginRequiredException;
	}

	/**
	 * 获取异常的简短描述
	 * @param e 异常对象
	 * @return 异常描述
	 */
	public String getExceptionDescription(Exception e) {
		if (e instanceof IpBlockedException) {
			return "IP被封禁";
		}
		else if (e instanceof RateLimitException) {
			return "API限流";
		}
		else if (e instanceof LoginRequiredException) {
			return "需要重新登录";
		}
		else if (e instanceof DataFetchException) {
			return "数据获取失败";
		}
		else if (e instanceof CrawlerException) {
			return "爬虫执行异常";
		}
		else {
			return "未知异常";
		}
	}

}