package com.libre.spider.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 测试配置类 用于@WebMvcTest，排除数据层组件
 */
@TestConfiguration
@ComponentScan(basePackages = { "com.libre.spider.controller", "com.libre.spider.common" },
		excludeFilters = { @ComponentScan.Filter(pattern = "com.libre.spider.mapper.*"),
				@ComponentScan.Filter(pattern = "com.libre.spider.service.*"),
				@ComponentScan.Filter(pattern = "com.libre.spider.config.MyBatisConfig") })
public class TestConfig {

}
