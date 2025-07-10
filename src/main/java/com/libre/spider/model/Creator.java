package com.libre.spider.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 创作者信息
 */
@Data
public class Creator {

	/**
	 * 用户ID
	 */
	@JsonProperty("user_id")
	private String userId;

	/**
	 * 昵称
	 */
	private String nickname;

	/**
	 * 头像
	 */
	private String avatar;

	/**
	 * 个人简介
	 */
	private String desc;

	/**
	 * 性别
	 */
	private String gender;

	/**
	 * 粉丝数
	 */
	private Integer fans;

	/**
	 * 获赞与收藏数
	 */
	private Integer interaction;

	/**
	 * 关注数
	 */
	private Integer follows;

	/**
	 * 笔记数
	 */
	private Integer notes;

	/**
	 * 专辑数
	 */
	private Integer boards;

	/**
	 * 收藏数
	 */
	private Integer collects;

	/**
	 * IP归属地
	 */
	@JsonProperty("ip_location")
	private String ipLocation;

	/**
	 * 是否品牌合作人
	 */
	@JsonProperty("is_brand_cooperation")
	private Boolean isBrandCooperation;

	/**
	 * 个人主页链接
	 */
	@JsonProperty("homepage_url")
	private String homepageUrl;

}