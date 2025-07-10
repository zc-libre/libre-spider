package com.libre.spider.service;

import com.libre.spider.config.XhsConfig;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Playwright管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaywrightService {

	private final XhsConfig xhsConfig;

	private final CookieService cookieService;

	private final JavaScriptExecutor jsExecutor;

	private Playwright playwright;

	private Browser browser;

	private BrowserContext context;

	private Page page;

	private boolean resourcesCleaned = false;

	@PostConstruct
	public void init() {
		if (playwright != null) {
			return; // 已经初始化
		}
		log.info("初始化Playwright...");
		playwright = Playwright.create();
		log.info("Playwright初始化完成");
	}

	/**
	 * 启动浏览器
	 */
	public void launchBrowser(boolean headless) {
		if (resourcesCleaned) {
			throw new IllegalStateException("服务资源已被清理，无法启动浏览器");
		}

		if (browser != null && browser.isConnected()) {
			return;
		}

		if (playwright == null) {
			throw new IllegalStateException("Playwright未初始化");
		}

		// 关闭之前的浏览器（如果存在）
		if (browser != null) {
			try {
				browser.close();
			}
			catch (Exception e) {
				log.warn("关闭之前的浏览器失败", e);
			}
		}

		BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(headless)
			.setArgs(Arrays.asList("--disable-blink-features=AutomationControlled", "--no-sandbox",
					"--disable-dev-shm-usage"));

		browser = playwright.chromium().launch(options); // 改用Chrome提高性能
		log.info("浏览器启动成功，headless={}", headless);
	}

	/**
	 * 创建浏览器上下文
	 */
	public BrowserContext createContext() {
		if (resourcesCleaned) {
			throw new IllegalStateException("服务资源已被清理，无法创建上下文");
		}

		if (browser == null || !browser.isConnected()) {
			throw new IllegalStateException("浏览器未启动或已断开连接");
		}

		// 关闭之前的上下文（如果存在）
		if (context != null) {
			try {
				// 先关闭上下文中的所有页面
				for (Page p : context.pages()) {
					try {
						if (!p.isClosed()) {
							p.close();
						}
					}
					catch (Exception e) {
						log.warn("关闭页面失败", e);
					}
				}
				context.close();
			}
			catch (Exception e) {
				log.warn("关闭之前的浏览器上下文失败", e);
			}
		}

		Browser.NewContextOptions contextOptions = new Browser.NewContextOptions().setViewportSize(1920, 1080)
			.setUserAgent(
					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
			.setLocale("zh-CN");

		// 如果启用CDP模式，可以在这里配置
		if (xhsConfig.isEnableCdpMode()) {
			// CDP模式配置
		}

		context = browser.newContext(contextOptions);

		// 设置Cookie
		cookieService.setCookiesToContext(context);

		log.info("浏览器上下文创建成功");
		return context;
	}

	/**
	 * 创建新页面
	 */
	public Page createPage() {
		if (resourcesCleaned) {
			throw new IllegalStateException("服务资源已被清理，无法创建页面");
		}

		if (context == null) {
			throw new IllegalStateException("浏览器上下文未创建");
		}

		// 关闭之前的页面（如果存在）
		if (page != null && !page.isClosed()) {
			try {
				page.close();
			}
			catch (Exception e) {
				log.warn("关闭之前的页面失败", e);
			}
		}

		page = context.newPage();

		// 注入反检测脚本
		try {
			jsExecutor.injectStealthScript(page);
		}
		catch (Exception e) {
			log.warn("注入stealth脚本失败: {}", e.getMessage());
		}

		// 延迟导航，避免页面过早关闭
		// 让调用方决定何时导航
		log.info("页面创建成功");
		return page;
	}

	/**
	 * 获取当前页面
	 */
	public Page getPage() {
		if (resourcesCleaned || page == null || page.isClosed()) {
			throw new IllegalStateException("页面未初始化、已关闭或服务已清理");
		}
		return page;
	}

	/**
	 * 获取浏览器上下文
	 */
	public BrowserContext getContext() {
		if (resourcesCleaned || context == null) {
			throw new IllegalStateException("浏览器上下文未初始化或服务已清理");
		}
		return context;
	}

	/**
	 * 登录检查
	 */
	public boolean checkLogin() {
		if (resourcesCleaned || page == null || page.isClosed() || context == null) {
			log.warn("无法检查登录状态：页面或上下文未初始化");
			return false;
		}

		try {
			String oldWebSession = cookieService.getWebSession();

			// 刷新页面以更新Cookie
			page.reload();
			page.waitForLoadState(LoadState.NETWORKIDLE);

			// 从浏览器更新Cookie
			cookieService.updateFromContext(context);

			return cookieService.checkLoginState(oldWebSession);
		}
		catch (Exception e) {
			log.error("登录检查失败", e);
			return false;
		}
	}

	@PreDestroy
	public void cleanup() {
		if (resourcesCleaned) {
			return; // 已经清理过了
		}

		log.info("开始清理Playwright资源...");
		resourcesCleaned = true;

		// 按顺序清理资源：页面 -> 上下文 -> 浏览器 -> Playwright
		if (page != null && !page.isClosed()) {
			try {
				page.close();
				log.info("页面已关闭");
			}
			catch (Exception e) {
				log.error("关闭页面失败", e);
			}
		}

		if (context != null) {
			try {
				// 关闭上下文中的所有页面
				for (Page p : context.pages()) {
					try {
						if (!p.isClosed()) {
							p.close();
						}
					}
					catch (Exception e) {
						log.warn("关闭页面失败", e);
					}
				}
				context.close();
				log.info("浏览器上下文已关闭");
			}
			catch (Exception e) {
				log.error("关闭浏览器上下文失败", e);
			}
		}

		if (browser != null) {
			try {
				browser.close();
				log.info("浏览器已关闭");
			}
			catch (Exception e) {
				log.error("关闭浏览器失败", e);
			}
		}

		if (playwright != null) {
			try {
				playwright.close();
				log.info("Playwright已关闭");
			}
			catch (Exception e) {
				log.error("关闭Playwright失败", e);
			}
		}

		// 清空引用
		page = null;
		context = null;
		browser = null;
		playwright = null;

		log.info("Playwright资源清理完成");
	}

}