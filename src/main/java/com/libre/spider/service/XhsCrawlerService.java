package com.libre.spider.service;

import com.libre.spider.client.XhsApiClient;
import com.libre.spider.config.XhsConfig;
import com.libre.spider.enums.SearchSortType;
import com.libre.spider.model.*;
import com.libre.spider.utils.XhsSignatureHelper;
import com.microsoft.playwright.Page;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 小红书爬虫主服务 负责协调各个组件，类似 Python 项目中的 XiaoHongShuCrawler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XhsCrawlerService {

	private final XhsConfig xhsConfig;

	private final PlaywrightService playwrightService;

	private final CookieService cookieService;

	private final JavaScriptExecutor jsExecutor;

	private final XhsApiClient apiClient;

	private final XhsSignatureHelper signatureHelper;

	private final ConcurrencyControlService concurrencyControlService;

	private final DataStorageService dataStorageService;

	private Page page;

	private boolean initialized = false;

	/**
	 * 服务启动后初始化
	 */
	@PostConstruct
	public void init() {
		if (initialized) {
			return;
		}

		try {
			log.info("开始初始化小红书爬虫服务...");

			// 1. 启动浏览器
			playwrightService.launchBrowser(true); // headless模式

			// 2. 创建浏览器上下文
			playwrightService.createContext();

			// 3. 创建页面
			page = playwrightService.createPage();

			// 4. 设置页面给 API 客户端
			apiClient.setPlaywrightPage(page);

			// 5. 设置页面给 JavaScript 执行器
			jsExecutor.setDefaultPage(page);

			// 6. 加载 Cookie（如果有配置）
			if (!xhsConfig.getCookies().isEmpty()) {
				cookieService.loadFromConfig();
				log.info("已加载配置的 Cookie");
			}

			// 7. 导航到小红书首页
			try {
				page.navigate(xhsConfig.getWebDomain());
				page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
				log.info("已导航到小红书首页");
			}
			catch (Exception e) {
				log.warn("导航到小红书首页失败: {}", e.getMessage());
			}

			initialized = true;
			log.info("小红书爬虫服务初始化完成");
		}
		catch (Exception e) {
			log.error("初始化小红书爬虫服务失败", e);
			throw new RuntimeException("Failed to initialize XHS crawler service", e);
		}
	}

	/**
	 * 服务停止前清理
	 */
	@PreDestroy
	public void cleanup() {
		log.info("开始清理小红书爬虫服务资源...");

		// 清理页面
		if (page != null && !page.isClosed()) {
			try {
				page.close();
				log.info("页面已关闭");
			}
			catch (Exception e) {
				log.error("关闭页面失败", e);
			}
		}

		// 委托PlaywrightService清理其他资源
		try {
			playwrightService.cleanup();
			log.info("浏览器资源已清理");
		}
		catch (Exception e) {
			log.error("清理浏览器资源失败", e);
		}

		initialized = false;
		page = null;
		log.info("小红书爬虫服务资源清理完成");
	}

	/**
	 * 搜索笔记
	 */
	public SearchResponse searchNotes(String keyword, int page) throws Exception {
		return searchNotes(keyword, page, SearchSortType.GENERAL);
	}

	/**
	 * 搜索笔记（指定排序方式）
	 */
	public SearchResponse searchNotes(String keyword, int page, SearchSortType sortType) throws Exception {
		log.info("搜索关键词: {}, 页码: {}, 排序: {}", keyword, page, sortType.name());

		// 生成搜索ID - 使用签名帮助器生成，与Python版本保持一致
		String searchId = signatureHelper.getSearchId();
		log.debug("生成搜索ID: {}", searchId);

		// 调用 API 搜索
		return apiClient.searchNotes(keyword, searchId, page, sortType);
	}

	/**
	 * 获取笔记详情（指定xsecSource和xsecToken）
	 */
	public Map<String, Object> getNoteDetail(String noteId, String xsecSource, String xsecToken) throws Exception {
		log.info("获取笔记详情: {}, xsecSource: {}, xsecToken: {}", noteId, xsecSource, xsecToken);
		return apiClient.getNoteById(noteId, xsecSource, xsecToken);
	}

	/**
	 * 从笔记URL解析笔记信息
	 */
	public Map<String, String> parseNoteInfoFromUrl(String noteUrl) {
		try {
			// 正则表达式解析笔记URL中的参数
			// 格式:
			// https://www.xiaohongshu.com/explore/noteId?xsec_token=xxx&xsec_source=xxx
			String regex = "https://www\\.xiaohongshu\\.com/explore/([^?]+)(?:\\?.*xsec_token=([^&]+).*xsec_source=([^&]+))?";
			java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
			java.util.regex.Matcher matcher = pattern.matcher(noteUrl);

			Map<String, String> result = new HashMap<>();
			if (matcher.find()) {
				result.put("noteId", matcher.group(1));
				result.put("xsecToken", matcher.group(2) != null ? matcher.group(2) : "");
				result.put("xsecSource", matcher.group(3) != null ? matcher.group(3) : "");
			}
			return result;
		}
		catch (Exception e) {
			log.error("解析笔记URL失败: {}", e.getMessage());
			return new HashMap<>();
		}
	}

	/**
	 * 获取用户信息
	 */
	public Map<String, Object> getUserInfo(String userId) throws Exception {
		log.info("获取用户信息: {}", userId);
		return apiClient.getUserInfo(userId);
	}

	/**
	 * 从搜索结果中提取有效笔记项（过滤掉推荐查询等）
	 */
	public List<SearchItem> extractValidNoteItems(SearchResponse searchResponse) {
		if (searchResponse == null || searchResponse.getData() == null || searchResponse.getData().getItems() == null) {
			return List.of();
		}

		return searchResponse.getData()
			.getItems()
			.stream()
			.filter(SearchItem::isValidNoteItem)
			.collect(Collectors.toList());
	}

	/**
	 * 从搜索结果批量构造笔记URL
	 */
	public List<String> extractNoteUrlsFromSearchResult(SearchResponse searchResponse) {
		return extractValidNoteItems(searchResponse).stream()
			.map(item -> item.buildNoteUrl(xhsConfig.getWebDomain()))
			.filter(url -> url != null)
			.collect(Collectors.toList());
	}

	/**
	 * 从搜索结果批量获取笔记详情
	 */
	public List<Map<String, Object>> batchGetNoteDetailsFromSearch(SearchResponse searchResponse) throws Exception {
		List<SearchItem> validItems = extractValidNoteItems(searchResponse);
		log.info("从搜索结果中找到 {} 个有效笔记", validItems.size());

		List<Map<String, Object>> noteDetails = new java.util.ArrayList<>();

		for (SearchItem item : validItems) {
			try {
				log.debug("获取笔记详情: {}", item.getId());
				Map<String, Object> noteDetail = apiClient.getNoteById(item.getId(),
						item.getXsecSource() != null ? item.getXsecSource() : "pc_search",
						item.getXsecToken() != null ? item.getXsecToken() : "");

				if (noteDetail != null && !noteDetail.isEmpty()) {
					// 添加原始搜索信息
					noteDetail.put("search_item_id", item.getId());
					noteDetail.put("search_url", item.buildNoteUrl(xhsConfig.getWebDomain()));
					noteDetails.add(noteDetail);
				}

				// 控制请求频率，避免被封
				Thread.sleep(3000);

			}
			catch (Exception e) {
				log.warn("获取笔记详情失败，笔记ID: {}, 错误: {}", item.getId(), e.getMessage());
			}
		}

		log.info("成功获取 {} 个笔记详情", noteDetails.size());
		return noteDetails;
	}

	/**
	 * 通用的搜索和获取详情方法 - 重构后的版本，调用已有服务
	 */
	public CrawlerResult searchAndFetchDetails(CrawlerRequest request) {
		try {
			log.info("开始执行爬取任务: keyword={}, maxPages={}, enableDetail={}, enableUser={}", request.getKeyword(),
					request.getMaxPages(), request.getEnableNoteDetail(), request.getEnableUserInfo());

			// 步骤1: 搜索笔记
			List<SearchItem> allSearchItems = searchMultiplePages(request);
			if (allSearchItems.isEmpty()) {
				return CrawlerResult.failure("未搜索到任何笔记");
			}

			// 步骤2: 去重处理（调用已有服务）
			List<SearchItem> deduplicatedItems = processDeduplication(allSearchItems, request.getEnableDeduplication());

			// 步骤3: 获取笔记详情（重构为独立方法）
			NoteDetailResult detailResult = fetchNoteDetails(deduplicatedItems, request);

			// 步骤4: 获取用户信息（调用已有服务）
			UserInfoResult userResult = fetchUserInfos(detailResult.getNoteCards(), request.getEnableUserInfo());

			log.info("爬取任务完成: keyword={}, totalFound={}, success={}, failed={}, users={}", request.getKeyword(),
					allSearchItems.size(), detailResult.getSuccessCount(), detailResult.getFailedCount(),
					userResult.getSuccessCount());

			return CrawlerResult.builder()
				.success(true)
				.keyword(request.getKeyword())
				.actualPages(calculateActualPages(request, allSearchItems.size()))
				.totalFound(allSearchItems.size())
				.afterDeduplication(deduplicatedItems.size())
				.successNoteDetails(detailResult.getSuccessCount())
				.failedNoteDetails(detailResult.getFailedCount())
				.successUserInfos(userResult.getSuccessCount())
				.searchItems(allSearchItems)
				.noteCards(detailResult.getNoteCards())
				.noteDetails(detailResult.getNoteDetails())
				.users(userResult.getUsers())
				.build();

		}
		catch (Exception e) {
			log.error("爬取任务失败: keyword={}", request.getKeyword(), e);
			return CrawlerResult.failure(e.getMessage());
		}
	}

	/**
	 * 搜索多页数据
	 */
	private List<SearchItem> searchMultiplePages(CrawlerRequest request) {
		List<SearchItem> allItems = new ArrayList<>();

		try {
			for (int page = request.getStartPage(); page < request.getStartPage() + request.getMaxPages(); page++) {
				log.info("搜索第{}页，关键词: {}", page, request.getKeyword());

				SearchResponse response = searchNotes(request.getKeyword(), page, request.getSortType());
				if (response == null || !Boolean.TRUE.equals(response.getSuccess()) || response.getData() == null
						|| response.getData().getItems() == null || response.getData().getItems().isEmpty()) {
					log.info("第{}页无更多数据，停止搜索", page);
					break;
				}

				List<SearchItem> pageItems = extractValidNoteItems(response);
				allItems.addAll(pageItems);
				log.info("第{}页获取笔记数量: {}", page, pageItems.size());

				// 检查是否达到数量限制
				if (request.getMaxNotesPerKeyword() != null && allItems.size() >= request.getMaxNotesPerKeyword()) {
					allItems = allItems.subList(0, request.getMaxNotesPerKeyword());
					log.info("达到最大笔记数量限制: {}", request.getMaxNotesPerKeyword());
					break;
				}

				// 页面间隔
				if (page < request.getStartPage() + request.getMaxPages() - 1) {
					Thread.sleep(request.getRequestInterval());
				}
			}
		}
		catch (Exception e) {
			log.error("搜索多页数据失败: keyword={}", request.getKeyword(), e);
		}

		return allItems;
	}

	/**
	 * 去重处理 - 调用已有的数据存储服务
	 */
	private List<SearchItem> processDeduplication(List<SearchItem> searchItems, Boolean enableDeduplication) {
		if (!Boolean.TRUE.equals(enableDeduplication)) {
			return searchItems;
		}

		log.info("开始去重处理，原始数量: {}", searchItems.size());
		List<SearchItem> deduplicatedItems = searchItems.stream()
			.filter(item -> item.getId() != null)
			.filter(item -> !dataStorageService.isNoteExists(item.getId()))
			.collect(Collectors.toList());
		log.info("去重处理完成，去重后数量: {}", deduplicatedItems.size());
		return deduplicatedItems;
	}

	/**
	 * 获取笔记详情 - 使用并发控制服务
	 */
	private NoteDetailResult fetchNoteDetails(List<SearchItem> searchItems, CrawlerRequest request) {
		if (!Boolean.TRUE.equals(request.getEnableNoteDetail())) {
			// 不获取详情，直接返回搜索结果
			List<NoteCard> noteCards = searchItems.stream()
				.map(SearchItem::getNoteCard)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
			return new NoteDetailResult(noteCards, new ArrayList<>(), noteCards.size(), 0);
		}

		log.info("开始批量获取笔记详情，总数: {}", searchItems.size());
		List<NoteCard> noteCards = new ArrayList<>();
		List<Map<String, Object>> noteDetails = new ArrayList<>();
		int successCount = 0;
		int failedCount = 0;

		// 分批处理，使用并发控制
		int batchSize = request.getConcurrency();
		for (int i = 0; i < searchItems.size(); i += batchSize) {
			int endIndex = Math.min(i + batchSize, searchItems.size());
			List<SearchItem> batch = searchItems.subList(i, endIndex);

			log.debug("处理笔记详情批次: {}-{}/{}", i + 1, endIndex, searchItems.size());

			// 处理当前批次
			for (SearchItem item : batch) {
				if (processSingleNoteDetail(item, request, noteCards, noteDetails)) {
					successCount++;
				}
				else {
					failedCount++;
				}
			}

			// 批次间隔
			if (endIndex < searchItems.size()) {
				try {
					Thread.sleep(request.getRequestInterval());
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					log.warn("线程被中断");
				}
			}
		}

		log.info("批量获取笔记详情完成: success={}, failed={}", successCount, failedCount);
		return new NoteDetailResult(noteCards, noteDetails, successCount, failedCount);
	}

	/**
	 * 处理单个笔记详情获取
	 */
	private boolean processSingleNoteDetail(SearchItem item, CrawlerRequest request, List<NoteCard> noteCards,
			List<Map<String, Object>> noteDetails) {
		// 获取并发许可证
		if (!concurrencyControlService.acquirePermit(5000)) {
			log.warn("获取并发许可证超时: {}", item.getId());
			concurrencyControlService.recordFailure();
			addNoteCardIfExists(item, noteCards);
			return false;
		}

		try {
			String xsecSource = item.getXsecSource() != null ? item.getXsecSource() : "pc_search";
			String xsecToken = item.getXsecToken() != null ? item.getXsecToken() : "";

			Map<String, Object> noteDetail = apiClient.getNoteById(item.getId(), xsecSource, xsecToken);
			if (noteDetail != null && !noteDetail.isEmpty()) {
				noteDetails.add(noteDetail);
				addNoteCardIfExists(item, noteCards);
				concurrencyControlService.recordSuccess();
				return true;
			}
			else {
				concurrencyControlService.recordFailure();
				addNoteCardIfExists(item, noteCards);
				return false;
			}
		}
		catch (Exception e) {
			log.warn("获取笔记详情失败: noteId={}, error={}", item.getId(), e.getMessage());
			concurrencyControlService.recordFailure();
			addNoteCardIfExists(item, noteCards);
			return false;
		}
		finally {
			concurrencyControlService.releasePermit();
			try {
				Thread.sleep(request.getRequestInterval());
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * 获取用户信息 - 调用已有服务
	 */
	private UserInfoResult fetchUserInfos(List<NoteCard> noteCards, Boolean enableUserInfo) {
		if (!Boolean.TRUE.equals(enableUserInfo)) {
			return new UserInfoResult(new ArrayList<>(), 0);
		}

		// 提取唯一的用户ID
		Set<String> userIds = noteCards.stream()
			.map(NoteCard::getUser)
			.filter(Objects::nonNull)
			.map(User::getUserId)
			.filter(Objects::nonNull)
			.filter(userId -> !dataStorageService.isUserExists(userId))
			.collect(Collectors.toSet());

		log.info("需要获取用户信息数量: {}", userIds.size());

		List<User> users = new ArrayList<>();
		int successCount = 0;

		for (String userId : userIds) {
			try {
				Map<String, Object> userInfo = getUserInfo(userId);
				if (userInfo != null && !userInfo.isEmpty()) {
					// 这里可以转换为User对象，暂时跳过
					successCount++;
				}
			}
			catch (Exception e) {
				log.warn("获取用户信息失败: userId={}, error={}", userId, e.getMessage());
			}
		}

		log.info("批量获取用户信息完成: success={}", successCount);
		return new UserInfoResult(users, successCount);
	}

	/**
	 * 辅助方法：添加笔记卡片（如果存在）
	 */
	private void addNoteCardIfExists(SearchItem item, List<NoteCard> noteCards) {
		if (item.getNoteCard() != null) {
			noteCards.add(item.getNoteCard());
		}
	}

	/**
	 * 计算实际搜索的页数
	 */
	private Integer calculateActualPages(CrawlerRequest request, int totalFound) {
		if (request.getMaxNotesPerKeyword() != null && totalFound >= request.getMaxNotesPerKeyword()) {
			// 如果达到数量限制，可能没有搜索完所有页
			return Math.min(request.getMaxPages(), (int) Math.ceil((double) request.getMaxNotesPerKeyword() / 20));
		}
		return request.getMaxPages();
	}

	/**
	 * 笔记详情结果内部类
	 */
	private static class NoteDetailResult {

		private final List<NoteCard> noteCards;

		private final List<Map<String, Object>> noteDetails;

		private final int successCount;

		private final int failedCount;

		public NoteDetailResult(List<NoteCard> noteCards, List<Map<String, Object>> noteDetails, int successCount,
				int failedCount) {
			this.noteCards = noteCards;
			this.noteDetails = noteDetails;
			this.successCount = successCount;
			this.failedCount = failedCount;
		}

		public List<NoteCard> getNoteCards() {
			return noteCards;
		}

		public List<Map<String, Object>> getNoteDetails() {
			return noteDetails;
		}

		public int getSuccessCount() {
			return successCount;
		}

		public int getFailedCount() {
			return failedCount;
		}

	}

	/**
	 * 用户信息结果内部类
	 */
	private static class UserInfoResult {

		private final List<User> users;

		private final int successCount;

		public UserInfoResult(List<User> users, int successCount) {
			this.users = users;
			this.successCount = successCount;
		}

		public List<User> getUsers() {
			return users;
		}

		public int getSuccessCount() {
			return successCount;
		}

	}

	/**
	 * 检查登录状态
	 */
	public boolean checkLoginStatus() {
		if (!initialized || page == null || page.isClosed()) {
			log.warn("服务未初始化或页面已关闭");
			return false;
		}

		try {
			// 可以通过访问个人页面或API来检查登录状态
			page.navigate(xhsConfig.getWebDomain() + "/user/profile");
			// 检查是否跳转到登录页面
			String currentUrl = page.url();
			return !currentUrl.contains("/login");
		}
		catch (Exception e) {
			log.error("检查登录状态失败", e);
			return false;
		}
	}

	/**
	 * 获取当前页面
	 */
	public Page getPage() {
		if (!initialized || page == null || page.isClosed()) {
			throw new IllegalStateException("服务未初始化或页面已关闭，无法获取页面");
		}
		return page;
	}

}