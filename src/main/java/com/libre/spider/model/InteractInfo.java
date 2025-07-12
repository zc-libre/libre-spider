package com.libre.spider.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 小红书互动信息实体类
 */
@Data
public class InteractInfo {

	/**
	 * 点赞数
	 */
	@JsonProperty("liked_count")
	private String likedCount;

	/**
	 * 是否已收藏
	 */
	private Boolean collected;

	/**
	 * 收藏数
	 */
	@JsonProperty("collected_count")
	private String collectedCount;

	/**
	 * 评论数
	 */
	@JsonProperty("comment_count")
	private String commentCount;

	/**
	 * 分享数
	 */
	@JsonProperty("shared_count")
	private String sharedCount;

	/**
	 * 是否已点赞
	 */
	private Boolean liked;

}
