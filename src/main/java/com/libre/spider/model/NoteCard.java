package com.libre.spider.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * 小红书笔记卡片实体类
 */
@Data
public class NoteCard {

	/**
	 * 封面信息
	 */
	private Cover cover;

	/**
	 * 图片列表
	 */
	@JsonProperty("image_list")
	private List<ImageInfo> imageList;

	/**
	 * 角标信息列表
	 */
	@JsonProperty("corner_tag_info")
	private List<CornerTagInfo> cornerTagInfo;

	/**
	 * 笔记类型
	 */
	private String type;

	/**
	 * 显示标题
	 */
	@JsonProperty("display_title")
	private String displayTitle;

	/**
	 * 用户信息
	 */
	private User user;

	/**
	 * 互动信息
	 */
	@JsonProperty("interact_info")
	private InteractInfo interactInfo;

	/**
	 * 笔记ID
	 */
	@JsonProperty("note_id")
	private String noteId;

	/**
	 * 标题
	 */
	private String title;

	/**
	 * 标签列表
	 */
	@JsonProperty("tag_list")
	private List<String> tagList;

}
