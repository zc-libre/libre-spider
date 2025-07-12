package com.libre.spider.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libre.spider.config.XhsConfig;
import com.libre.spider.enums.SearchSortType;
import com.libre.spider.exception.XhsException;
import com.libre.spider.model.SearchResponse;
import com.libre.spider.service.CookieService;
import com.libre.spider.service.JavaScriptExecutor;
import com.libre.spider.utils.XhsSignatureHelper;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.http.HttpRequest;
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
		return HttpRequest.post(url).addHeader(headers).bodyString(jsonData).execute().asValue(typeRef);
	}

	/**
	 * 搜索笔记 - 与Python版本保持一致
	 */
	public SearchResponse searchNotes(String keyword, String searchId, int page, SearchSortType sortType)
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

		return post(url, data, new TypeReference<>() {
		});
	}

	/**
	 * 获取笔记详情（三级降级策略，完全按照MediaCrawler实现）
	 */
	public Map<String, Object> getNoteById(String noteId, String xsecSource, String xsecToken) throws IOException {
		// 第一优先级：HTML解析方式（携带Cookie）
		Map<String, Object> noteDetail = getNoteByIdFromHtml(noteId, xsecSource, xsecToken, true);

		// 第二优先级：HTML解析方式（不携带Cookie）
		if (noteDetail == null || noteDetail.isEmpty()) {
			log.debug("HTML解析（带Cookie）失败，尝试HTML解析（不带Cookie）");
			noteDetail = getNoteByIdFromHtml(noteId, xsecSource, xsecToken, false);
		}

		// 第三优先级：API方式（降级）
		if (noteDetail == null || noteDetail.isEmpty()) {
			log.debug("HTML解析失败，尝试API方式");
			noteDetail = getNoteByIdFromApi(noteId, xsecSource, xsecToken);
		}

		// 添加重要的安全参数
		if (noteDetail != null && !noteDetail.isEmpty()) {
			noteDetail.put("xsec_token", xsecToken);
			noteDetail.put("xsec_source", xsecSource);
		}

		return noteDetail;
	}

	/**
	 * 通过HTML解析获取笔记详情（优先策略）
	 */
	private Map<String, Object> getNoteByIdFromHtml(String noteId, String xsecSource, String xsecToken,
			boolean enableCookie) {
		try {
			String url = xhsConfig.getWebDomain() + "/explore/" + noteId + "?xsec_token=" + xsecToken + "&xsec_source="
					+ xsecSource;

			// 构建完整的请求头，包括签名
			Map<String, String> headers = buildHeaders(url, null);

			// 添加HTML请求特有的请求头
			headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");

			// 根据参数决定是否添加Cookie
			if (!enableCookie) {
				headers.remove("Cookie");
			}

			Request.Builder requestBuilder = new Request.Builder().url(url).get();
			headers.forEach(requestBuilder::addHeader);

			log.debug("HTML请求URL: {}", url);
			log.debug("HTML请求头: {}", headers);

			try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
				if (!response.isSuccessful()) {
					log.warn("HTML页面请求失败，状态码: {}", response.code());
					return null;
				}

				String html = response.body().string();

				// 解析HTML中的window.__INITIAL_STATE__
				return parseNoteFromHtml(html, noteId);
			}
		}
		catch (Exception e) {
			log.error("HTML解析方式获取笔记详情失败: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * 通过API获取笔记详情（降级策略）
	 */
	private Map<String, Object> getNoteByIdFromApi(String noteId, String xsecSource, String xsecToken) {
		try {
			String url = xhsConfig.getApiDomain() + "/api/sns/web/v1/feed";

			Map<String, Object> data = new LinkedHashMap<>();
			data.put("source_note_id", noteId);
			data.put("image_formats", new String[] { "jpg", "webp", "avif" });
			data.put("extra", Map.of("need_body_topic", 1));
			data.put("xsec_source", xsecSource);
			data.put("xsec_token", xsecToken);

			Map<String, Object> response = post(url, data, new TypeReference<Map<String, Object>>() {
			});

			// 解析API响应
			if (response != null && response.containsKey("data") && response.get("data") != null) {
				Map<String, Object> responseData = (Map<String, Object>) response.get("data");
				if (responseData.containsKey("items")) {
					java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) responseData
						.get("items");
					if (items != null && !items.isEmpty()) {
						Map<String, Object> item = items.get(0);
						if (item.containsKey("note_card")) {
							return (Map<String, Object>) item.get("note_card");
						}
					}
				}
			}

			return null;
		}
		catch (Exception e) {
			log.error("API方式获取笔记详情失败: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * 解析HTML中的笔记信息
	 */
	private Map<String, Object> parseNoteFromHtml(String html, String noteId) {
		try {
			// 使用正则表达式提取window.__INITIAL_STATE__（与Python版本保持一致）
			String regex = "window\\.__INITIAL_STATE__=(\\{.*\\})</script>";
			java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
			java.util.regex.Matcher matcher = pattern.matcher(html);

			if (matcher.find()) {
				// 与Python版本完全一致的字符串处理
				String jsonStr = matcher.group(1).replace("undefined", "\"\"").replace("\\u002F", "/");

				// 检查是否为空JSON
				if ("{}".equals(jsonStr)) {
					return new HashMap<>();
				}

				Map<String, Object> initialState = objectMapper.readValue(jsonStr,
						new TypeReference<Map<String, Object>>() {
						});

				// 转换所有键名为下划线格式（与Python版本的transform_json_keys一致）
				Map<String, Object> transformedState = camelToUnderscore(initialState);

				// 按照MediaCrawler的路径解析：note_dict["note"]["note_detail_map"][note_id]["note"]
				Map<String, Object> noteSection = (Map<String, Object>) transformedState.get("note");
				if (noteSection != null && noteSection.containsKey("note_detail_map")) {
					Map<String, Object> noteDetailMap = (Map<String, Object>) noteSection.get("note_detail_map");
					if (noteDetailMap != null && noteDetailMap.containsKey(noteId)) {
						Map<String, Object> noteInfo = (Map<String, Object>) noteDetailMap.get(noteId);
						if (noteInfo != null && noteInfo.containsKey("note")) {
							return (Map<String, Object>) noteInfo.get("note");
						}
					}
				}
			}

			return null;
		}
		catch (Exception e) {
			log.error("解析HTML中的笔记信息失败: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * 将驼峰命名转换为下划线命名（与MediaCrawler保持一致）
	 */
	private Map<String, Object> camelToUnderscore(Map<String, Object> map) {
		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String key = entry.getKey();
			String newKey = key.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
			Object value = entry.getValue();

			if (value instanceof Map) {
				result.put(newKey, camelToUnderscore((Map<String, Object>) value));
			}
			else if (value instanceof java.util.List) {
				java.util.List<?> list = (java.util.List<?>) value;
				java.util.List<Object> newList = new java.util.ArrayList<>();
				for (Object item : list) {
					if (item instanceof Map) {
						newList.add(camelToUnderscore((Map<String, Object>) item));
					}
					else {
						newList.add(item);
					}
				}
				result.put(newKey, newList);
			}
			else {
				result.put(newKey, value);
			}
		}
		return result;
	}

	/**
	 * 获取用户信息（通过解析HTML，完全按照MediaCrawler实现）
	 */
	public Map<String, Object> getUserInfo(String userId) throws IOException {
		return getUserInfoWithRetry(userId, 3);
	}

	/**
	 * 获取用户信息（带重试机制）
	 */
	private Map<String, Object> getUserInfoWithRetry(String userId, int maxRetries) throws IOException {
		Exception lastException = null;

		for (int attempt = 1; attempt <= maxRetries; attempt++) {
			try {
				return getUserInfoOnce(userId);
			}
			catch (Exception e) {
				lastException = e;
				log.warn("获取用户信息失败，第{}次尝试，错误: {}", attempt, e.getMessage());

				if (attempt < maxRetries) {
					try {
						Thread.sleep(1000); // 1秒间隔，与MediaCrawler保持一致
					}
					catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new IOException("重试被中断", ie);
					}
				}
			}
		}

		throw new IOException("获取用户信息失败，已重试" + maxRetries + "次", lastException);
	}

	/**
	 * 单次获取用户信息
	 */
	private Map<String, Object> getUserInfoOnce(String userId) throws IOException {
		String url = xhsConfig.getWebDomain() + "/user/profile/" + userId;

		log.info("request url: {}", url);
		// 复用现有的buildHeaders方法，但需要修改Accept头适配HTML页面请求
		Map<String, String> headers = buildHeaders(url, null);
		headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");

		Request.Builder requestBuilder = new Request.Builder().url(url).get();
		headers.forEach(requestBuilder::addHeader);

		try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
			// 检查状态码，处理特殊情况
			if (response.code() == 471 || response.code() == 461) {
				String verifyType = response.header("Verifytype");
				String verifyUuid = response.header("Verifyuuid");
				throw new XhsException.VerificationError("出现验证码，请求失败。验证类型: " + verifyType + ", UUID: " + verifyUuid);
			}

			if (!response.isSuccessful()) {
				throw new XhsException.DataFetchError("请求失败，状态码: " + response.code());
			}

			String html = response.body().string();

			// 解析HTML中的window.__INITIAL_STATE__
			return parseUserInfoFromHtml(html);
		}
	}

	/**
	 * 解析HTML中的用户信息
	 */
	private Map<String, Object> parseUserInfoFromHtml(String html) throws IOException {
		try {
			// 使用正则表达式提取window.__INITIAL_STATE__ (与MediaCrawler Python版本保持一致)
			String regex = "<script>window\\.__INITIAL_STATE__=(.+)<\\/script>";
			java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.MULTILINE);
			java.util.regex.Matcher matcher = pattern.matcher(html);

			if (matcher.find()) {
				String jsonStr = matcher.group(1).replace(":undefined", ":null").replace("\\u002F", "/");

				Map<String, Object> initialState = objectMapper.readValue(jsonStr, new TypeReference<>() {
				});

				// 按照MediaCrawler的路径解析：info.get("user").get("userPageData")
				Map<String, Object> userDict = (Map<String, Object>) initialState.get("user");
				if (userDict != null && userDict.containsKey("userPageData")) {
					Map<String, Object> userPageData = (Map<String, Object>) userDict.get("userPageData");

					// 标准化用户信息数据结构（与MediaCrawler保持一致）
					return standardizeUserInfo(userPageData);
				}
			}

			throw new IOException("无法从HTML中解析用户信息");
		}
		catch (Exception e) {
			throw new IOException("解析用户信息失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 标准化用户信息数据结构（与MediaCrawler保持一致）
	 */
	private Map<String, Object> standardizeUserInfo(Map<String, Object> userPageData) {
		Map<String, Object> result = new HashMap<>();

		// 基本信息
		if (userPageData.containsKey("basicInfo")) {
			Map<String, Object> basicInfo = (Map<String, Object>) userPageData.get("basicInfo");
			result.put("nickname", basicInfo.get("nickname"));
			result.put("gender", getGenderString(basicInfo.get("gender")));
			result.put("images", basicInfo.get("images")); // 头像
			result.put("desc", basicInfo.get("desc")); // 描述
			result.put("ipLocation", basicInfo.get("ipLocation")); // IP位置
		}

		// 交互数据
		if (userPageData.containsKey("interactions")) {
			java.util.List<Map<String, Object>> interactions = (java.util.List<Map<String, Object>>) userPageData
				.get("interactions");
			int follows = 0, fans = 0, interaction = 0;

			for (Map<String, Object> item : interactions) {
				String type = (String) item.get("type");
				Object countObj = item.get("count");
				int count = countObj instanceof Integer ? (Integer) countObj : 0;

				switch (type) {
					case "follows":
						follows = count;
						break;
					case "fans":
						fans = count;
						break;
					case "interaction":
						interaction = count;
						break;
				}
			}

			result.put("follows", follows);
			result.put("fans", fans);
			result.put("interaction", interaction);
		}

		// 标签信息
		if (userPageData.containsKey("tags")) {
			java.util.List<Map<String, Object>> tags = (java.util.List<Map<String, Object>>) userPageData.get("tags");
			Map<String, String> tagMap = new HashMap<>();

			for (Map<String, Object> tag : tags) {
				String tagType = (String) tag.get("tagType");
				String tagName = (String) tag.get("name");
				if (tagType != null && tagName != null) {
					tagMap.put(tagType, tagName);
				}
			}

			result.put("tagList", tagMap);
		}

		// 添加时间戳
		result.put("lastModifyTs", System.currentTimeMillis());

		return result;
	}

	/**
	 * 获取性别字符串（与MediaCrawler Python版本保持一致）
	 */
	private String getGenderString(Object gender) {
		if (gender == null)
			return null; // 与Python版本保持一致，返回null而不是"未知"

		if (gender instanceof Number) {
			int genderValue = ((Number) gender).intValue();
			if (genderValue == 1) {
				return "女"; // Python版本：1对应女性
			}
			else if (genderValue == 0) {
				return "男"; // Python版本：0对应男性
			}
			else {
				return null; // 与Python版本保持一致
			}
		}

		return gender.toString();
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
				httpClient.dispatcher().executorService();
				httpClient.dispatcher().executorService().shutdown();
				log.info("HTTP客户端资源已清理");
			}
			catch (Exception e) {
				log.error("清理HTTP客户端资源失败", e);
			}
		}
	}

}