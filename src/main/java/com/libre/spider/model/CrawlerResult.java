package com.libre.spider.model;

import lombok.Data;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * 爬虫结果
 */
@Data
@Builder
public class CrawlerResult {

	/**
	 * 是否成功
	 */
	private Boolean success;

	/**
	 * 错误信息
	 */
	private String errorMessage;

	/**
	 * 搜索关键词
	 */
	private String keyword;

	/**
	 * 实际搜索的页数
	 */
	private Integer actualPages;

	/**
	 * 总搜索到的笔记数
	 */
	private Integer totalFound;

	/**
	 * 去重后的笔记数
	 */
	private Integer afterDeduplication;

	/**
	 * 成功获取详情的笔记数
	 */
	private Integer successNoteDetails;

	/**
	 * 失败的笔记数
	 */
	private Integer failedNoteDetails;

	/**
	 * 成功获取的用户数
	 */
	private Integer successUserInfos;

	/**
	 * 搜索结果原始数据
	 */
	private List<SearchItem> searchItems;

	/**
	 * 笔记详情数据
	 */
	private List<NoteCard> noteCards;

	/**
	 * 笔记详情API结果（用于兼容现有接口）
	 */
	private List<Map<String, Object>> noteDetails;

	/**
	 * 用户信息数据
	 */
	private List<User> users;

	/**
	 * 处理统计信息
	 */
	private Map<String, Object> statistics;

	/**
	 * 创建成功结果
	 */
	public static CrawlerResult success() {
		return CrawlerResult.builder().success(true).build();
	}

	/**
	 * 创建失败结果
	 */
	public static CrawlerResult failure(String errorMessage) {
		return CrawlerResult.builder().success(false).errorMessage(errorMessage).build();
	}

	/**
	 * 获取处理统计信息的Map（用于API返回）
	 */
	public Map<String, Object> toApiResponse() {
		Map<String, Object> response = new java.util.HashMap<>();
		response.put("success", success);
		response.put("keyword", keyword);
		response.put("actualPages", actualPages);
		response.put("totalFound", totalFound);
		response.put("afterDeduplication", afterDeduplication);
		response.put("successNoteDetails", successNoteDetails);
		response.put("failedNoteDetails", failedNoteDetails);
		response.put("successUserInfos", successUserInfos);

		if (errorMessage != null) {
			response.put("error", errorMessage);
		}
		if (searchItems != null) {
			response.put("searchItems", searchItems);
		}
		if (noteCards != null) {
			response.put("noteCards", noteCards);
		}
		if (noteDetails != null) {
			response.put("noteDetails", noteDetails);
		}
		if (users != null) {
			response.put("users", users);
		}
		if (statistics != null) {
			response.putAll(statistics);
		}

		return response;
	}

}