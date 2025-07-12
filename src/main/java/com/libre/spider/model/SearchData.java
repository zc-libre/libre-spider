package com.libre.spider.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * 小红书搜索数据实体类
 */
@Data
public class SearchData {

	/**
	 * 是否有更多数据
	 */
	@JsonProperty("has_more")
	private Boolean hasMore;

	/**
	 * 搜索结果项列表
	 */
	private List<SearchItem> items;

}
