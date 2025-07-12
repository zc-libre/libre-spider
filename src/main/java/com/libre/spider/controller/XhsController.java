package com.libre.spider.controller;

import com.libre.spider.enums.SearchSortType;
import com.libre.spider.model.*;
import com.libre.spider.service.XhsCrawlerService;
import com.libre.spider.utils.ValidationUtils;
import com.libre.spider.utils.ExceptionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 小红书爬虫 REST API 控制器 提供小红书数据爬取的REST接口
 */
@Slf4j
@RestController
@RequestMapping("/api/xhs")
@RequiredArgsConstructor
public class XhsController {

	private final XhsCrawlerService crawlerService;

	private final ValidationUtils validationUtils;

	private final ExceptionHandler exceptionHandler;

	/**
	 * 搜索笔记
	 * @param keyword 搜索关键词
	 * @param page 页码（默认1）
	 * @param sortType 排序类型（默认综合排序）
	 * @return 搜索结果
	 */
	@GetMapping("/search")
	public SearchResponse searchNotes(@RequestParam("keyword") String keyword,
			@RequestParam(defaultValue = "1") Integer page,
			@RequestParam(defaultValue = "GENERAL") SearchSortType sortType) {
		try {
			// 参数验证
			Map<String, Object> keywordValidation = validationUtils.validateKeyword(keyword);
			if (!(Boolean) keywordValidation.get("success")) {
				SearchResponse errorResponse = new SearchResponse();
				errorResponse.setSuccess(false);
				errorResponse.setMsg((String) keywordValidation.get("error"));
				errorResponse.setCode(-1);
				return errorResponse;
			}

			Map<String, Object> pageValidation = validationUtils.validatePage(page);
			if (!(Boolean) pageValidation.get("success")) {
				SearchResponse errorResponse = new SearchResponse();
				errorResponse.setSuccess(false);
				errorResponse.setMsg((String) pageValidation.get("error"));
				errorResponse.setCode(-1);
				return errorResponse;
			}

			return crawlerService.searchNotes(keyword, page, sortType);
		}
		catch (Exception e) {
			exceptionHandler.handleTaskException(e, "搜索笔记");
			// 返回错误响应
			SearchResponse errorResponse = new SearchResponse();
			errorResponse.setSuccess(false);
			errorResponse.setMsg(e.getMessage());
			errorResponse.setCode(-1);
			return errorResponse;
		}
	}

	/**
	 * 获取笔记详情
	 * @param noteId 笔记ID
	 * @param xsecSource 安全参数（可选）
	 * @param xsecToken 安全参数（可选）
	 * @return 笔记详情
	 */
	@GetMapping("/note/{noteId}")
	public Map<String, Object> getNoteDetail(@PathVariable String noteId,
			@RequestParam(required = false, defaultValue = "") String xsecSource,
			@RequestParam(required = false, defaultValue = "") String xsecToken) {
		try {
			// 参数验证
			Map<String, Object> validation = validationUtils.validateNoteId(noteId);
			if (!(Boolean) validation.get("success")) {
				return validation;
			}

			return crawlerService.getNoteDetail(noteId, xsecSource, xsecToken);
		}
		catch (Exception e) {
			exceptionHandler.handleTaskException(e, "获取笔记详情");
			return Map.of("success", false, "error", e.getMessage());
		}
	}

	/**
	 * 从笔记URL解析笔记信息
	 * @param request 包含笔记URL的请求体
	 * @return 解析结果和笔记详情
	 */
	@PostMapping("/note/parse")
	public Map<String, Object> parseNoteUrl(@RequestBody Map<String, String> request) {
		try {
			String noteUrl = request.get("noteUrl");

			// 参数验证
			Map<String, Object> validation = validationUtils.validateNoteUrl(noteUrl);
			if (!(Boolean) validation.get("success")) {
				return validation;
			}

			Map<String, String> noteInfo = crawlerService.parseNoteInfoFromUrl(noteUrl);
			if (noteInfo.isEmpty()) {
				return Map.of("success", false, "error", "无法解析笔记URL");
			}

			// 如果解析成功，直接获取笔记详情
			String noteId = noteInfo.get("noteId");
			String xsecSource = noteInfo.get("xsecSource");
			String xsecToken = noteInfo.get("xsecToken");

			Map<String, Object> noteDetail = crawlerService.getNoteDetail(noteId, xsecSource, xsecToken);

			return Map.of("success", true, "noteInfo", noteInfo, "noteDetail", noteDetail);
		}
		catch (Exception e) {
			exceptionHandler.handleTaskException(e, "解析笔记URL");
			return Map.of("success", false, "error", e.getMessage());
		}
	}

	/**
	 * 获取用户信息
	 * @param userId 用户ID
	 * @return 用户信息
	 */
	@GetMapping("/user/{userId}")
	public Map<String, Object> getUserInfo(@PathVariable String userId) {
		try {
			// 参数验证
			Map<String, Object> validation = validationUtils.validateUserId(userId);
			if (!(Boolean) validation.get("success")) {
				return validation;
			}

			return crawlerService.getUserInfo(userId);
		}
		catch (Exception e) {
			exceptionHandler.handleTaskException(e, "获取用户信息");
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

	/**
	 * 从搜索结果中提取笔记URL
	 * @param keyword 搜索关键词
	 * @param page 页码（默认1）
	 * @param sortType 排序类型（默认综合排序）
	 */
	@GetMapping("/extract-urls")
	public Map<String, Object> extractNoteUrls(@RequestParam("keyword") String keyword,
			@RequestParam(defaultValue = "1") Integer page,
			@RequestParam(defaultValue = "GENERAL") SearchSortType sortType) {
		try {
			// 先执行搜索
			SearchResponse searchResponse = crawlerService.searchNotes(keyword, page, sortType);

			if (searchResponse.getSuccess() == null || !searchResponse.getSuccess()) {
				return Map.of("success", false, "error", "搜索失败: " + searchResponse.getMsg());
			}

			// 提取有效笔记项
			List<SearchItem> validItems = crawlerService.extractValidNoteItems(searchResponse);

			// 构造URL列表
			List<String> noteUrls = crawlerService.extractNoteUrlsFromSearchResult(searchResponse);

			return Map.of("success", true, "keyword", keyword, "page", page, "totalFound", validItems.size(),
					"noteUrls", noteUrls, "validItems", validItems);
		}
		catch (Exception e) {
			log.error("提取笔记URL失败", e);
			return Map.of("success", false, "error", e.getMessage());
		}
	}

	/**
	 * 搜索并批量获取笔记详情（使用通用爬取方法）
	 * @param keyword 搜索关键词
	 * @param page 页码（默认1）
	 * @param sortType 排序类型（默认综合排序）
	 */
	@GetMapping("/search-and-fetch")
	public Map<String, Object> searchAndFetchNoteDetails(@RequestParam("keyword") String keyword,
			@RequestParam(value = "page", defaultValue = "1") Integer page,
			@RequestParam(value = "sortType", defaultValue = "GENERAL") SearchSortType sortType) {
		try {
			// 使用通用爬取方法
			CrawlerRequest request = CrawlerRequest.singlePage(keyword, page, sortType);
			CrawlerResult result = crawlerService.searchAndFetchDetails(request);

			if (!result.getSuccess()) {
				return Map.of("success", false, "error", result.getErrorMessage());
			}

			// 返回兼容格式，保持原有API的响应结构
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("keyword", keyword);
			response.put("page", page);
			response.put("sortType", sortType);
			response.put("totalFound", result.getTotalFound());
			response.put("successNoteDetails", result.getSuccessNoteDetails());
			response.put("failedNoteDetails", result.getFailedNoteDetails());

			// 保持原有字段名以兼容现有代码
			if (result.getNoteDetails() != null) {
				response.put("noteDetails", result.getNoteDetails());
				response.put("totalFetched", result.getNoteDetails().size());
			}
			if (result.getSearchItems() != null) {
				response.put("searchItems", result.getSearchItems());
			}
			if (result.getNoteCards() != null) {
				response.put("noteCards", result.getNoteCards());
			}

			return response;
		}
		catch (Exception e) {
			log.error("搜索并获取笔记详情失败", e);
			return Map.of("success", false, "error", e.getMessage());
		}
	}

}