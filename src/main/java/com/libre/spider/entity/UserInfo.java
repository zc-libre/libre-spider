package com.libre.spider.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息实体类
 */
@Data
@TableName("user_info")
public class UserInfo {

	@TableId(type = IdType.AUTO)
	private Long id;

	/**
	 * 用户ID
	 */
	@TableField("user_id")
	private String userId;

	/**
	 * 用户昵称
	 */
	@TableField("nickname")
	private String nickname;

	/**
	 * 用户头像URL
	 */
	@TableField("avatar")
	private String avatar;

	/**
	 * 用户简介
	 */
	@TableField("description")
	private String description;

	/**
	 * 用户主页URL
	 */
	@TableField("profile_url")
	private String profileUrl;

	/**
	 * 粉丝数
	 */
	@TableField("followers_count")
	private Integer followersCount;

	/**
	 * 关注数
	 */
	@TableField("following_count")
	private Integer followingCount;

	/**
	 * 获赞数
	 */
	@TableField("like_count")
	private Integer likeCount;

	/**
	 * 笔记数
	 */
	@TableField("note_count")
	private Integer noteCount;

	/**
	 * 标签信息（JSON字符串）
	 */
	@TableField("tags")
	private String tags;

	/**
	 * 地理位置
	 */
	@TableField("location")
	private String location;

	/**
	 * 性别：male(男), female(女), unknown(未知)
	 */
	@TableField("gender")
	private String gender;

	/**
	 * 是否认证用户
	 */
	@TableField("is_verified")
	private Boolean isVerified;

	/**
	 * 认证信息
	 */
	@TableField("verification_info")
	private String verificationInfo;

	/**
	 * 任务ID
	 */
	@TableField("task_id")
	private Long taskId;

	/**
	 * 创建时间
	 */
	@TableField("create_time")
	private LocalDateTime createTime;

	/**
	 * 更新时间
	 */
	@TableField("update_time")
	private LocalDateTime updateTime;

}