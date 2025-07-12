package com.libre.spider.model;

import com.libre.spider.enums.SearchSortType;
import lombok.Data;
import lombok.Builder;

/**
 * 爬虫请求参数
 */
@Data
@Builder
public class CrawlerRequest {

	/**
	 * 搜索关键词
	 */
	private String keyword;

	/**
	 * 起始页码（默认1）
	 */
	@Builder.Default
	private Integer startPage = 1;

	/**
	 * 最大页数（默认1）
	 */
	@Builder.Default
	private Integer maxPages = 1;

	/**
	 * 排序类型（默认综合排序）
	 */
	@Builder.Default
	private SearchSortType sortType = SearchSortType.GENERAL;

	/**
	 * 每个关键词最大笔记数量（默认无限制）
	 */
	private Integer maxNotesPerKeyword;

	/**
	 * 是否获取笔记详情（默认true）
	 */
	@Builder.Default
	private Boolean enableNoteDetail = true;

	/**
	 * 是否获取用户信息（默认false）
	 */
	@Builder.Default
	private Boolean enableUserInfo = false;

	/**
	 * 是否启用去重（默认true）
	 */
	@Builder.Default
	private Boolean enableDeduplication = true;

	/**
	 * 并发数（默认3）
	 */
	@Builder.Default
	private Integer concurrency = 3;

	/**
	 * 请求间隔（毫秒，默认1000）
	 */
	@Builder.Default
	private Long requestInterval = 1000L;

	/**
	 * 创建单页请求
	 */
	public static CrawlerRequest singlePage(String keyword, Integer page, SearchSortType sortType) {
		return CrawlerRequest.builder()
			.keyword(keyword)
			.startPage(page)
			.maxPages(1)
			.sortType(sortType)
			.enableNoteDetail(true)
			.enableUserInfo(false)
			.enableDeduplication(false) // 单页通常不需要去重
			.build();
	}

	/**
	 * 创建批量请求
	 */
	public static CrawlerRequest batch(String keyword, Integer maxPages, SearchSortType sortType) {
		return CrawlerRequest.builder()
			.keyword(keyword)
			.startPage(1)
			.maxPages(maxPages)
			.sortType(sortType)
			.enableNoteDetail(true)
			.enableUserInfo(false)
			.enableDeduplication(true)
			.build();
	}

}