package com.libre.spider.model;

import lombok.Data;

/**
 * 小红书图片信息实体类
 */
@Data
public class ImageInfo {

	/**
	 * 图片高度
	 */
	private Integer height;

	/**
	 * 图片宽度
	 */
	private Integer width;

	/**
	 * 图片URL
	 */
	private String url;

}
