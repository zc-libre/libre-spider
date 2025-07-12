package com.libre.spider.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 小红书用户信息实体类
 */
@Data
public class User {

	/**
	 * 昵称
	 */
	private String nickname;

	/**
	 * xsec_token
	 */
	@JsonProperty("xsec_token")
	private String xsecToken;

	/**
	 * 昵称（备用字段）
	 */
	@JsonProperty("nick_name")
	private String nickName;

	/**
	 * 头像URL
	 */
	private String avatar;

	/**
	 * 用户ID
	 */
	@JsonProperty("user_id")
	private String userId;

	/**
	 * 用户描述
	 */
	private String desc;

	/**
	 * 粉丝数
	 */
	@JsonProperty("followers_count")
	private Integer followersCount;

	/**
	 * 关注数
	 */
	@JsonProperty("following_count")
	private Integer followingCount;

	/**
	 * 获赞数
	 */
	@JsonProperty("like_count")
	private Integer likeCount;

	/**
	 * 笔记数
	 */
	@JsonProperty("note_count")
	private Integer noteCount;

	/**
	 * 标签列表
	 */
	private java.util.List<String> tags;

}
