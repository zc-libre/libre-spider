package com.libre.spider.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记信息实体类
 */
@Data
@TableName("note_info")
public class NoteInfo {

	@TableId(type = IdType.AUTO)
	private Long id;

	/**
	 * 笔记ID
	 */
	@TableField("note_id")
	private String noteId;

	/**
	 * 笔记标题
	 */
	@TableField("title")
	private String title;

	/**
	 * 笔记内容
	 */
	@TableField("content")
	private String content;

	/**
	 * 笔记类型：normal(普通笔记), video(视频笔记)
	 */
	@TableField("note_type")
	private String noteType;

	/**
	 * 用户ID
	 */
	@TableField("user_id")
	private String userId;

	/**
	 * 用户昵称
	 */
	@TableField("user_name")
	private String userName;

	/**
	 * 用户头像URL
	 */
	@TableField("user_avatar")
	private String userAvatar;

	/**
	 * 封面图片URL
	 */
	@TableField("cover_url")
	private String coverUrl;

	/**
	 * 图片URLs（JSON字符串）
	 */
	@TableField("image_urls")
	private String imageUrls;

	/**
	 * 视频URL
	 */
	@TableField("video_url")
	private String videoUrl;

	/**
	 * 点赞数
	 */
	@TableField("like_count")
	private Integer likeCount;

	/**
	 * 评论数
	 */
	@TableField("comment_count")
	private Integer commentCount;

	/**
	 * 收藏数
	 */
	@TableField("collect_count")
	private Integer collectCount;

	/**
	 * 分享数
	 */
	@TableField("share_count")
	private Integer shareCount;

	/**
	 * 笔记URL
	 */
	@TableField("note_url")
	private String noteUrl;

	/**
	 * 发布时间
	 */
	@TableField("publish_time")
	private LocalDateTime publishTime;

	/**
	 * 关键词
	 */
	@TableField("keyword")
	private String keyword;

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