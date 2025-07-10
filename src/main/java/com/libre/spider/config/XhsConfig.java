package com.libre.spider.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 小红书爬虫配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "xhs")
public class XhsConfig {

	/**
	 * Cookie字符串（必须包含 web_session）
	 */
	private String cookies = "";

	/**
	 * API域名
	 */
	private String apiDomain = "https://edith.xiaohongshu.com";

	/**
	 * 网站域名
	 */
	private String webDomain = "https://www.xiaohongshu.com";

	/**
	 * 是否启用CDP模式（Chrome DevTools Protocol）
	 */
	private boolean enableCdpMode = false;

}