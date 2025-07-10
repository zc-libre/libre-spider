package com.libre.spider.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libre.spider.config.XhsConfig;
import com.libre.spider.enums.SearchSortType;
import com.libre.spider.service.CookieService;
import com.libre.spider.service.JavaScriptExecutor;
import com.libre.spider.utils.XhsSignatureHelper;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PreDestroy;

/**
 * 小红书API客户端
 */
@Slf4j
@Component
public class XhsApiClient {

	private final XhsConfig xhsConfig;

	private final CookieService cookieService;

	private final JavaScriptExecutor jsExecutor;

	private final XhsSignatureHelper signatureHelper;

	private final ObjectMapper objectMapper;

	private final ObjectMapper compactObjectMapper; // 用于生成紧凑格式的JSON

	private final OkHttpClient httpClient;

	private Page playwrightPage;

	public XhsApiClient(XhsConfig xhsConfig, CookieService cookieService, JavaScriptExecutor jsExecutor,
			XhsSignatureHelper signatureHelper, ObjectMapper objectMapper) {
		this.xhsConfig = xhsConfig;
		this.cookieService = cookieService;
		this.jsExecutor = jsExecutor;
		this.signatureHelper = signatureHelper;
		this.objectMapper = objectMapper;

		// 创建一个用于生成紧凑JSON的ObjectMapper，与Python版本保持一致
		this.compactObjectMapper = new ObjectMapper();
		this.compactObjectMapper.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN,
				true);

		this.httpClient = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS) // 连接超时缩短
			.readTimeout(30, TimeUnit.SECONDS) // 读取超时保持
			.writeTimeout(10, TimeUnit.SECONDS) // 写入超时缩短
			.connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES)) // 连接池配置
			.build();
	}

	/**
	 * 设置Playwright页面对象
	 */
	public void setPlaywrightPage(Page page) {
		this.playwrightPage = page;
	}

	/**
	 * 构建请求头
	 */
	private Map<String, String> buildHeaders(String url, Object data) {
		Map<String, String> headers = new HashMap<>();

		// 基础请求头
		headers.put("User-Agent",
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
		headers.put("Accept", "application/json, text/plain, */*");
		headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
		headers.put("Content-Type", "application/json;charset=UTF-8");
		headers.put("Origin", xhsConfig.getWebDomain());
		headers.put("Referer", xhsConfig.getWebDomain() + "/");

		// 添加Cookie
		headers.put("Cookie", cookieService.getCookieString());

		// 获取加密参数
		if (playwrightPage != null) {
			try {
				Map<String, Object> encryptParams = jsExecutor.getEncryptParams(playwrightPage, url, data);
				String xS = (String) encryptParams.get("X-s");
				String xT = String.valueOf(encryptParams.get("X-t"));
				String b1 = jsExecutor.getB1FromLocalStorage(playwrightPage);
				String a1 = cookieService.getA1();

				// 生成签名
				Map<String, String> signs = signatureHelper.sign(a1, b1, xS, xT);
				headers.putAll(signs);

				log.debug("生成请求头签名: X-S={}, X-T={}, x-S-Common={}", signs.get("X-S"), signs.get("X-T"),
						signs.get("x-S-Common"));
			}
			catch (Exception e) {
				log.error("生成请求头签名失败", e);
			}
		}

		return headers;
	}

	/**
	 * 发送GET请求
	 */
	public <T> T get(String url, TypeReference<T> typeRef) throws IOException {
		Map<String, String> headers = buildHeaders(url, null);

		Request.Builder requestBuilder = new Request.Builder().url(url).get();

		headers.forEach(requestBuilder::addHeader);

		try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected response code: " + response.code());
			}

			String responseBody = response.body().string();
			log.debug("API响应: {}", responseBody);

			return objectMapper.readValue(responseBody, typeRef);
		}
	}

	/**
	 * 发送POST请求
	 */
	public <T> T post(String url, Object data, TypeReference<T> typeRef) throws IOException {
		Map<String, String> headers = buildHeaders(url, data);

		// 使用紧凑的JSON格式，与Python版本保持一致: json.dumps(data, separators=(',', ':'))
		String jsonData = compactObjectMapper.writeValueAsString(data);
		RequestBody body = RequestBody.create(jsonData, MediaType.parse("application/json"));

		Request.Builder requestBuilder = new Request.Builder().url(url).post(body);

		headers.forEach(requestBuilder::addHeader);

		try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected response code: " + response.code());
			}

			String responseBody = response.body().string();
			log.debug("API响应: {}", responseBody);

			return objectMapper.readValue(responseBody, typeRef);
		}
	}

	/**
	 * 搜索笔记 - 与Python版本保持一致
	 */
	public Map<String, Object> searchNotes(String keyword, String searchId, int page, SearchSortType sortType)
			throws IOException {
		String url = xhsConfig.getApiDomain() + "/api/sns/web/v1/search/notes";

		Map<String, Object> data = new LinkedHashMap<>(); // 使用LinkedHashMap保持顺序
		data.put("keyword", keyword);
		data.put("page", page);
		data.put("page_size", 20);
		data.put("search_id", searchId);
		data.put("sort", sortType.getValue());
		data.put("note_type", 0);
		// Python版本没有ext_flags和image_formats参数

		return post(url, data, new TypeReference<Map<String, Object>>() {
		});
	}

	/**
	 * 获取笔记详情（通过API）
	 */
	public Map<String, Object> getNoteById(String noteId, String xsecSource, String xsecToken) throws IOException {
		String url = xhsConfig.getApiDomain() + "/api/sns/web/v1/feed";

		Map<String, Object> data = new HashMap<>();
		data.put("source_note_id", noteId);
		data.put("image_formats", new String[] { "jpg", "webp", "avif" });
		data.put("extra", Map.of("need_body_topic", "1"));
		data.put("xsec_source", xsecSource);
		data.put("xsec_token", xsecToken);

		return post(url, data, new TypeReference<Map<String, Object>>() {
		});
	}

	/**
	 * 获取用户信息（通过解析HTML）
	 */
	public Map<String, Object> getUserInfo(String userId) throws IOException {
		String url = xhsConfig.getWebDomain() + "/user/profile/" + userId;
		Map<String, String> headers = buildHeaders(url, null);

		Request.Builder requestBuilder = new Request.Builder().url(url).get();

		headers.forEach(requestBuilder::addHeader);

		try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected response code: " + response.code());
			}

			String html = response.body().string();

			// 解析HTML中的window.__INITIAL_STATE__
			String regex = "<script>window.__INITIAL_STATE__=(.+?)</script>";
			java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
			java.util.regex.Matcher matcher = pattern.matcher(html);

			if (matcher.find()) {
				String jsonStr = matcher.group(1).replace(":undefined", ":null").replace("\\u002F", "/");

				Map<String, Object> initialState = objectMapper.readValue(jsonStr,
						new TypeReference<Map<String, Object>>() {
						});

				return (Map<String, Object>) ((Map<String, Object>) initialState.get("user")).get("userPageData");
			}

			throw new IOException("Failed to parse user info from HTML");
		}
	}

	/**
	 * 清理HTTP客户端资源
	 */
	@PreDestroy
	public void cleanup() {
		if (httpClient != null) {
			try {
				// 关闭连接池
				httpClient.connectionPool().evictAll();
				// 关闭调度器
				if (httpClient.dispatcher().executorService() != null) {
					httpClient.dispatcher().executorService().shutdown();
				}
				log.info("HTTP客户端资源已清理");
			}
			catch (Exception e) {
				log.error("清理HTTP客户端资源失败", e);
			}
		}
	}

}