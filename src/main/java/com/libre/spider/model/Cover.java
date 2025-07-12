package com.libre.spider.model;

import lombok.Data;

/**
 * 小红书笔记封面信息实体类
 */
@Data
public class Cover {

	/**
	 * 封面高度
	 */
	private Integer height;

	/**
	 * 封面宽度
	 */
	private Integer width;

	/**
	 * 图片URL
	 */
	private String url;

}
