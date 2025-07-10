package com.libre.spider.service;

import com.libre.spider.client.XhsApiClient;
import com.libre.spider.config.XhsConfig;
import com.libre.spider.enums.SearchSortType;
import com.libre.spider.utils.XhsSignatureHelper;
import com.microsoft.playwright.Page;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

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
	public Map<String, Object> searchNotes(String keyword, int page) throws Exception {
		return searchNotes(keyword, page, SearchSortType.GENERAL);
	}

	/**
	 * 搜索笔记（指定排序方式）
	 */
	public Map<String, Object> searchNotes(String keyword, int page, SearchSortType sortType) throws Exception {
		log.info("搜索关键词: {}, 页码: {}, 排序: {}", keyword, page, sortType.name());

		// 生成搜索ID - 使用签名帮助器生成，与Python版本保持一致
		String searchId = signatureHelper.getSearchId();
		log.debug("生成搜索ID: {}", searchId);

		// 调用 API 搜索
		return apiClient.searchNotes(keyword, searchId, page, sortType);
	}

	/**
	 * 获取笔记详情
	 */
	public Map<String, Object> getNoteDetail(String noteId) throws Exception {
		log.info("获取笔记详情: {}", noteId);
		// TODO: 需要从页面获取 xsecSource 和 xsecToken
		// 暂时使用空字符串，实际使用时需要从笔记页面解析这些参数
		return apiClient.getNoteById(noteId, "", "");
	}

	/**
	 * 获取用户信息
	 */
	public Map<String, Object> getUserInfo(String userId) throws Exception {
		log.info("获取用户信息: {}", userId);
		return apiClient.getUserInfo(userId);
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