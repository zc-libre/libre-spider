package com.libre.spider.enums;

/**
 * 搜索排序类型
 */
public enum SearchSortType {

	/**
	 * 综合排序
	 */
	GENERAL("general"),

	/**
	 * 最新发布
	 */
	TIME_DESCENDING("time_descending"),

	/**
	 * 最热排序
	 */
	POPULARITY_DESCENDING("popularity_descending");

	private final String value;

	SearchSortType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}