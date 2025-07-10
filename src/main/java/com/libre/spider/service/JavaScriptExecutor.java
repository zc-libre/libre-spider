package com.libre.spider.service;

import com.microsoft.playwright.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * JavaScript执行服务 - 使用Playwright执行JS获取加密参数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JavaScriptExecutor {

	private String stealthJs;

	private String xhsJs;

	private Page defaultPage;

	@PostConstruct
	public void init() {
		try {
			// 加载stealth.min.js
			ClassPathResource stealthResource = new ClassPathResource("stealth.min.js");
			stealthJs = IOUtils.toString(stealthResource.getInputStream(), StandardCharsets.UTF_8);

			// 加载xhsvm.js（如果有的话）
			try {
				ClassPathResource xhsResource = new ClassPathResource("xhsvm.js");
				xhsJs = IOUtils.toString(xhsResource.getInputStream(), StandardCharsets.UTF_8);
			}
			catch (IOException e) {
				log.warn("xhsvm.js文件未找到，将使用浏览器内置的加密函数");
			}
		}
		catch (IOException e) {
			log.error("加载JavaScript文件失败", e);
			throw new RuntimeException("Failed to load JavaScript files", e);
		}
	}

	/**
	 * 注入反检测脚本
	 */
	public void injectStealthScript(Page page) {
		if (stealthJs != null) {
			page.addInitScript(stealthJs);
			log.debug("已注入stealth.min.js反检测脚本");
		}
	}

	/**
	 * 注入小红书加密脚本
	 */
	public void injectXhsScript(Page page) {
		if (xhsJs != null) {
			page.evaluate(xhsJs);
			log.debug("已注入xhsvm.js加密脚本");
		}
	}

	/**
	 * 获取加密参数
	 * @param page Playwright页面对象
	 * @param url 请求URL
	 * @param data 请求数据（可选）
	 * @return 包含X-s和X-t的加密参数
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getEncryptParams(Page page, String url, Object data) {
		try {
			// 调用页面上的window._webmsxyw函数 - 与Python版本保持一致
			// Python: "([url, data]) => window._webmsxyw(url,data)", [url, data]
			Object result = page.evaluate("([url, data]) => window._webmsxyw(url,data)", new Object[] { url, data });

			if (result instanceof Map) {
				Map<String, Object> encryptParams = (Map<String, Object>) result;
				log.debug("获取加密参数成功: X-s={}, X-t={}", encryptParams.get("X-s"), encryptParams.get("X-t"));
				return encryptParams;
			}
			else {
				log.error("获取加密参数失败，返回类型不正确: {}", result);
				throw new RuntimeException("Invalid encrypt params type");
			}
		}
		catch (Exception e) {
			log.error("执行JavaScript获取加密参数失败", e);
			throw new RuntimeException("Failed to get encrypt params", e);
		}
	}

	/**
	 * 获取localStorage值 - 与Python版本保持一致 Python: await self.playwright_page.evaluate("() =>
	 * window.localStorage")
	 */
	@SuppressWarnings("unchecked")
	public Map<String, String> getLocalStorage(Page page) {
		try {
			// 获取所有localStorage项
			Object result = page
				.evaluate("() => {" + "const items = {};" + "for (let i = 0; i < window.localStorage.length; i++) {"
						+ "    const key = window.localStorage.key(i);"
						+ "    items[key] = window.localStorage.getItem(key);" + "}" + "return items;" + "}");
			if (result instanceof Map) {
				return (Map<String, String>) result;
			}
			log.warn("获取localStorage失败，返回空Map");
			return Map.of();
		}
		catch (Exception e) {
			log.error("获取localStorage失败", e);
			return Map.of();
		}
	}

	/**
	 * 获取b1值
	 */
	public String getB1FromLocalStorage(Page page) {
		Map<String, String> localStorage = getLocalStorage(page);
		return localStorage.getOrDefault("b1", "");
	}

	/**
	 * 设置默认页面
	 */
	public void setDefaultPage(Page page) {
		this.defaultPage = page;
	}

	/**
	 * 生成搜索ID - 使用Java实现，与Python版本保持一致 Python: e = int(time.time() * 1000) << 64 t =
	 * int(random.uniform(0, 2147483646)) return base36encode((e + t))
	 */
	public String getSearchId(Page page) {
		// 这个方法应该使用XhsSignatureHelper中的getSearchId方法
		// 而不是在JavaScript中生成
		log.warn("getSearchId应该使用XhsSignatureHelper.getSearchId()方法");
		return "";
	}

}