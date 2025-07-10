package com.libre.spider.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 小红书笔记
 */
@Data
public class Note {

	/**
	 * 笔记ID
	 */
	@JsonProperty("note_id")
	private String noteId;

	/**
	 * 笔记标题
	 */
	private String title;

	/**
	 * 笔记描述
	 */
	private String desc;

	/**
	 * 笔记类型: normal, video
	 */
	private String type;

	/**
	 * 作者ID
	 */
	@JsonProperty("user_id")
	private String userId;

	/**
	 * 作者昵称
	 */
	private String nickname;

	/**
	 * 作者头像
	 */
	private String avatar;

	/**
	 * 点赞数
	 */
	@JsonProperty("liked_count")
	private Integer likedCount;

	/**
	 * 收藏数
	 */
	@JsonProperty("collected_count")
	private Integer collectedCount;

	/**
	 * 评论数
	 */
	@JsonProperty("comment_count")
	private Integer commentCount;

	/**
	 * 分享数
	 */
	@JsonProperty("share_count")
	private Integer shareCount;

	/**
	 * 图片列表
	 */
	@JsonProperty("image_list")
	private List<Image> imageList;

	/**
	 * 标签列表
	 */
	@JsonProperty("tag_list")
	private List<Tag> tagList;

	/**
	 * 时间戳
	 */
	private Long timestamp;

	/**
	 * 最后修改时间
	 */
	@JsonProperty("last_update_time")
	private Long lastUpdateTime;

	/**
	 * IP归属地
	 */
	@JsonProperty("ip_location")
	private String ipLocation;

	/**
	 * 笔记链接
	 */
	@JsonProperty("note_url")
	private String noteUrl;

	/**
	 * xsec_source
	 */
	@JsonProperty("xsec_source")
	private String xsecSource;

	/**
	 * xsec_token
	 */
	@JsonProperty("xsec_token")
	private String xsecToken;

	/**
	 * 视频信息
	 */
	private Video video;

	/**
	 * 图片信息
	 */
	@Data
	public static class Image {

		private String url;

		@JsonProperty("trace_id")
		private String traceId;

		@JsonProperty("file_id")
		private String fileId;

		private Integer height;

		private Integer width;

		private String format;

	}

	/**
	 * 标签信息
	 */
	@Data
	public static class Tag {

		private String id;

		private String name;

		private String type;

	}

	/**
	 * 视频信息
	 */
	@Data
	public static class Video {

		private String url;

		private String cover;

		private Integer height;

		private Integer width;

		private Integer duration;

	}

}