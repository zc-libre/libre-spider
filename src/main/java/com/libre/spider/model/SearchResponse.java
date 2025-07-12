package com.libre.spider.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 小红书搜索接口响应实体类
 */
@Data
public class SearchResponse {

	/**
	 * 响应消息
	 */
	private String msg;

	/**
	 * 搜索数据
	 */
	private SearchData data;

	/**
	 * 响应代码
	 */
	private Integer code;

	/**
	 * 是否成功
	 */
	private Boolean success;

}
