package com.libre.spider.controller;

import com.libre.spider.enums.SearchSortType;
import com.libre.spider.service.XhsCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 小红书爬虫 REST API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/xhs")
@RequiredArgsConstructor
public class XhsController {

	private final XhsCrawlerService crawlerService;

	/**
	 * 搜索笔记
	 * @param keyword 搜索关键词
	 * @param page 页码（默认1）
	 * @param sortType 排序类型（默认综合排序）
	 */
	@GetMapping("/search")
	public Map<String, Object> searchNotes(@RequestParam("keyword") String keyword,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "GENERAL") SearchSortType sortType) {
		try {
			return crawlerService.searchNotes(keyword, page, sortType);
		}
		catch (Exception e) {
			log.error("搜索笔记失败", e);
			return Map.of("success", false, "error", e.getMessage());
		}
	}

	/**
	 * 获取笔记详情
	 * @param noteId 笔记ID
	 */
	@GetMapping("/note/{noteId}")
	public Map<String, Object> getNoteDetail(@PathVariable String noteId) {
		try {
			return crawlerService.getNoteDetail(noteId);
		}
		catch (Exception e) {
			log.error("获取笔记详情失败", e);
			return Map.of("success", false, "error", e.getMessage());
		}
	}

	/**
	 * 获取用户信息
	 * @param userId 用户ID
	 */
	@GetMapping("/user/{userId}")
	public Map<String, Object> getUserInfo(@PathVariable String userId) {
		try {
			return crawlerService.getUserInfo(userId);
		}
		catch (Exception e) {
			log.error("获取用户信息失败", e);
			return Map.of("success", false, "error", e.getMessage());
		}
	}

	/**
	 * 检查登录状态
	 */
	@GetMapping("/login/status")
	public Map<String, Object> checkLoginStatus() {
		boolean isLoggedIn = crawlerService.checkLoginStatus();
		return Map.of("success", true, "isLoggedIn", isLoggedIn, "message", isLoggedIn ? "已登录" : "未登录");
	}

}