package com.libre.spider.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 小红书搜索项实体类
 */
@Data
public class SearchItem {

	/**
	 * 笔记ID
	 */
	private String id;

	/**
	 * 模型类型
	 */
	@JsonProperty("model_type")
	private String modelType;

	/**
	 * 笔记卡片信息
	 */
	@JsonProperty("note_card")
	private NoteCard noteCard;

	/**
	 * xsec_token
	 */
	@JsonProperty("xsec_token")
	private String xsecToken;

	/**
	 * xsec_source
	 */
	@JsonProperty("xsec_source")
	private String xsecSource;

	/**
	 * 任务ID（用于边爬取边保存）
	 */
	private Long taskId;

	/**
	 * 关键词（用于边爬取边保存）
	 */
	private String keyword;

	/**
	 * 是否为有效的笔记项（过滤掉rec_query和hot_query）
	 */
	public boolean isValidNoteItem() {
		return modelType != null && !modelType.equals("rec_query") && !modelType.equals("hot_query");
	}

	/**
	 * 构造笔记详情页面URL
	 */
	public String buildNoteUrl(String webDomain) {
		if (id == null) {
			return null;
		}

		StringBuilder url = new StringBuilder(webDomain).append("/explore/").append(id);

		// 添加安全参数
		if (xsecToken != null || xsecSource != null) {
			url.append("?");
			if (xsecToken != null) {
				url.append("xsec_token=").append(xsecToken);
			}
			if (xsecSource != null) {
				if (xsecToken != null) {
					url.append("&");
				}
				url.append("xsec_source=").append(xsecSource);
			}
		}

		return url.toString();
	}

	/**
	 * 获取笔记信息映射（用于传递给笔记详情获取方法）
	 */
	public java.util.Map<String, String> toNoteInfo() {
		java.util.Map<String, String> noteInfo = new java.util.HashMap<>();
		noteInfo.put("noteId", id != null ? id : "");
		noteInfo.put("xsecToken", xsecToken != null ? xsecToken : "");
		noteInfo.put("xsecSource", xsecSource != null ? xsecSource : "pc_search"); // 默认值
		return noteInfo;
	}

}
