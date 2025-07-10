package com.libre.spider.service;

import com.libre.spider.config.XhsConfig;
import com.libre.spider.utils.CookieUtils;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.options.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cookie管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CookieService {

	private final XhsConfig xhsConfig;

	private final Map<String, String> cookieDict = new ConcurrentHashMap<>();

	/**
	 * 初始化Cookie
	 */
	public void initCookie(String cookieStr) {
		cookieDict.clear();
		cookieDict.putAll(CookieUtils.convertStrCookieToMap(cookieStr));
		log.info("Cookie初始化完成，包含{}个Cookie", cookieDict.size());
	}

	/**
	 * 从配置加载Cookie
	 */
	public void loadFromConfig() {
		if (xhsConfig.getCookies() != null && !xhsConfig.getCookies().isEmpty()) {
			initCookie(xhsConfig.getCookies());
		}
	}

	/**
	 * 设置Cookie到浏览器上下文
	 */
	public void setCookiesToContext(BrowserContext context) {
		cookieDict.forEach((name, value) -> {
			if ("web_session".equals(name)) {
				Cookie cookie = new Cookie(name, value);
				cookie.setDomain(".xiaohongshu.com");
				cookie.setPath("/");
				context.addCookies(List.of(cookie));
				log.info("已设置web_session cookie");
			}
		});
	}

	/**
	 * 从浏览器上下文获取Cookie
	 */
	public void updateFromContext(BrowserContext context) {
		var cookies = context.cookies();
		cookies.forEach(cookie -> {
			cookieDict.put(cookie.name, cookie.value);
		});
		log.info("从浏览器更新Cookie，当前包含{}个Cookie", cookieDict.size());
	}

	/**
	 * 获取Cookie值
	 */
	public String getCookieValue(String name) {
		return cookieDict.get(name);
	}

	/**
	 * 获取a1值
	 */
	public String getA1() {
		return cookieDict.getOrDefault("a1", "");
	}

	/**
	 * 获取web_session值
	 */
	public String getWebSession() {
		return cookieDict.getOrDefault("web_session", "");
	}

	/**
	 * 获取Cookie字符串
	 */
	public String getCookieString() {
		return CookieUtils.convertMapToCookieStr(cookieDict);
	}

	/**
	 * 检查登录状态
	 */
	public boolean checkLoginState(String oldWebSession) {
		String currentWebSession = getWebSession();
		return !currentWebSession.isEmpty() && !currentWebSession.equals(oldWebSession);
	}

}