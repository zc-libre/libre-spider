package com.libre.spider.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libre.spider.exception.CrawlerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.http.HttpRequest;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP请求模板工具类 提供统一的HTTP请求处理逻辑，避免重复代码
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpRequestTemplate {

	private final ObjectMapper objectMapper;

	private final ExceptionHandler exceptionHandler;

	/**
	 * 执行GET请求
	 * @param url 请求URL
	 * @param headers 请求头
	 * @param responseType 响应类型
	 * @param <T> 响应数据类型
	 * @return 响应数据
	 */
	public <T> T executeGet(String url, Map<String, String> headers, TypeReference<T> responseType) {
		return executeRequest("GET", url, headers, null, responseType);
	}

	/**
	 * 执行POST请求
	 * @param url 请求URL
	 * @param headers 请求头
	 * @param requestBody 请求体
	 * @param responseType 响应类型
	 * @param <T> 响应数据类型
	 * @return 响应数据
	 */
	public <T> T executePost(String url, Map<String, String> headers, String requestBody,
			TypeReference<T> responseType) {
		return executeRequest("POST", url, headers, requestBody, responseType);
	}

	/**
	 * 执行POST请求（JSON格式）
	 * @param url 请求URL
	 * @param headers 请求头
	 * @param requestData 请求数据对象
	 * @param responseType 响应类型
	 * @param <T> 响应数据类型
	 * @return 响应数据
	 */
	public <T> T executePostJson(String url, Map<String, String> headers, Object requestData,
			TypeReference<T> responseType) {
		try {
			String requestBody = objectMapper.writeValueAsString(requestData);
			return executeRequest("POST", url, headers, requestBody, responseType);
		}
		catch (Exception e) {
			exceptionHandler.handleHttpException(e, url, "POST");
			throw new CrawlerException("序列化请求数据失败", e);
		}
	}

	/**
	 * 通用请求执行方法
	 * @param method HTTP方法
	 * @param url 请求URL
	 * @param headers 请求头
	 * @param requestBody 请求体（可为null）
	 * @param responseType 响应类型
	 * @param <T> 响应数据类型
	 * @return 响应数据
	 */
	private <T> T executeRequest(String method, String url, Map<String, String> headers, String requestBody,
			TypeReference<T> responseType) {
		log.debug("发送{}请求: {}", method, url);
		try {
			HttpRequest httpRequest;
			if ("GET".equals(method)) {
				httpRequest = HttpRequest.get(url);
			}
			else {
				httpRequest = HttpRequest.post(url);
			}
			httpRequest.bodyString(requestBody);
			if (Objects.nonNull(headers)) {
				httpRequest.addHeader(headers);
			}
			return httpRequest.execute().asValue(responseType);
		}
		catch (Exception e) {
			exceptionHandler.handleHttpException(e, url, method);
			throw new CrawlerException(String.format("%s请求失败: %s", method, url), e);
		}
	}

	/**
	 * 构建HTTP请求
	 * @param method HTTP方法
	 * @param url 请求URL
	 * @param headers 请求头
	 * @param requestBody 请求体
	 * @return Request对象
	 */
	private Request buildRequest(String method, String url, Map<String, String> headers, String requestBody) {
		Request.Builder requestBuilder = new Request.Builder().url(url);

		// 添加请求头
		if (headers != null) {
			headers.forEach(requestBuilder::addHeader);
		}

		// 设置请求体
		if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
			RequestBody body = requestBody != null
					? RequestBody.create(requestBody, MediaType.get("application/json; charset=utf-8"))
					: RequestBody.create(new byte[0]);
			requestBuilder.method(method, body);
		}
		else {
			requestBuilder.method(method, null);
		}

		return requestBuilder.build();
	}

	/**
	 * 处理HTTP响应
	 * @param response 响应对象
	 * @param responseType 响应类型
	 * @param url 请求URL
	 * @param method HTTP方法
	 * @param <T> 响应数据类型
	 * @return 响应数据
	 */
	private <T> T handleResponse(Response response, TypeReference<T> responseType, String url, String method)
			throws IOException {
		if (!response.isSuccessful()) {
			String errorMsg = String.format("HTTP请求失败: %d %s", response.code(), response.message());
			log.error("{}请求失败 [{}]: {}", method, url, errorMsg);
			throw new IOException(errorMsg);
		}

		ResponseBody responseBody = response.body();
		if (responseBody == null) {
			throw new IOException("响应体为空");
		}

		String responseContent = responseBody.string();
		log.debug("{}请求成功 [{}]: 响应长度={}", method, url, responseContent.length());

		if (log.isTraceEnabled()) {
			log.trace("响应内容: {}", responseContent);
		}

		// 如果响应类型是String，直接返回
		if (responseType.getType().equals(String.class)) {
			return (T) responseContent;
		}

		// 解析JSON响应
		try {
			return objectMapper.readValue(responseContent, responseType);
		}
		catch (Exception e) {
			exceptionHandler.handleParseException(e, "JSON响应", url);
			throw new IOException("解析响应数据失败", e);
		}
	}

	/**
	 * 检查响应是否成功
	 * @param response HTTP响应
	 * @return 是否成功
	 */
	public boolean isSuccessful(Response response) {
		return response.isSuccessful();
	}

	/**
	 * 获取响应状态码
	 * @param response HTTP响应
	 * @return 状态码
	 */
	public int getStatusCode(Response response) {
		return response.code();
	}

}