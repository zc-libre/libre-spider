package com.libre.spider.service;

import com.libre.spider.config.CrawlerConfig;
import com.libre.spider.config.RetryConfig;
import com.libre.spider.exception.*;
import com.libre.spider.model.*;
import com.libre.spider.enums.SearchSortType;
import com.libre.spider.utils.ExceptionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 批量爬取服务 实现批量爬取流程：搜索 → 笔记详情 → 用户信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchCrawlerService {

	private final CrawlerConfig crawlerConfig;

	private final XhsCrawlerService xhsCrawlerService;

	private final DataStorageService dataStorageService;

	private final CrawlerStatusService crawlerStatusService;

	private final RetryConfig.RetryTemplate retryTemplate;

	private final ConcurrencyControlService concurrencyControlService;

	private final ExceptionHandler exceptionHandler;

	/**
	 * 执行批量爬取任务
	 */
	@Async
	public CompletableFuture<Void> executeBatchCrawl(String taskName, String keyword) {
		log.info("开始执行批量爬取任务: taskName={}, keyword={}", taskName, keyword);

		Long taskId = null;
		int totalNotes = 0;
		int successNotes = 0;
		int failedNotes = 0;
		String errorMessage = null;

		try {
			// 创建任务记录
			taskId = dataStorageService.createCrawlTask(taskName, keyword);
			dataStorageService.startCrawlTask(taskId);

			// 记录任务开始
			crawlerStatusService.recordTaskStart(taskName, keyword);

			// 使用边爬取边保存的方法
			log.info("步骤1: 搜索笔记");
			List<SearchItem> searchItems = searchNotes(keyword);
			totalNotes = searchItems.size();

			if (searchItems.isEmpty()) {
				throw new RuntimeException("未搜索到任何笔记");
			}

			// 去重处理
			if (crawlerConfig.getStorage().isEnableDeduplication()) {
				searchItems = deduplicateSearchItems(searchItems);
				log.info("去重后笔记数量: {}", searchItems.size());
			}

			// 设置taskId和keyword给每个SearchItem
			for (SearchItem item : searchItems) {
				item.setTaskId(taskId);
				item.setKeyword(keyword);
			}

			log.info("步骤2: 边爬取边保存笔记详情");
			List<NoteCard> noteCards = processNoteDetailsWithSave(searchItems, keyword, taskId);
			successNotes = noteCards.size();
			failedNotes = totalNotes - successNotes;

			// 步骤3: 批量获取用户信息（如果需要）
			if (crawlerConfig.isEnableUserInfo()) {
				log.info("步骤3: 获取用户信息");
				processUserInfoWithSave(noteCards, taskId);
			}

			log.info("批量爬取任务完成: taskName={}, total={}, success={}, failed={}", taskName, totalNotes, successNotes,
					failedNotes);

			// 记录任务完成
			int userCount = crawlerConfig.isEnableUserInfo()
					? noteCards.stream().mapToInt(note -> note.getUser() != null ? 1 : 0).sum() : 0;
			crawlerStatusService.recordTaskComplete(taskName, successNotes, userCount, failedNotes);

		}
		catch (Exception e) {
			String context = String.format("批量爬取任务失败: taskName=%s, keyword=%s", taskName, keyword);
			exceptionHandler.handleTaskException(e, context);
			errorMessage = e.getMessage();
			failedNotes = totalNotes - successNotes;

			// 记录任务失败
			crawlerStatusService.recordTaskFailure(taskName, errorMessage);
		}
		finally {
			if (taskId != null) {
				dataStorageService.completeCrawlTask(taskId, totalNotes, successNotes, failedNotes, errorMessage);
			}
		}

		return CompletableFuture.completedFuture(null);
	}

	/**
	 * 搜索笔记
	 */
	private List<SearchItem> searchNotes(String keyword) {
		List<SearchItem> allNotes = new ArrayList<>();
		int maxPages = crawlerConfig.getMaxPages();
		int maxNotesPerKeyword = crawlerConfig.getMaxNotesPerKeyword();

		try {
			for (int page = 1; page <= maxPages; page++) {
				log.info("搜索第{}页，关键词: {}", page, keyword);

				SearchResponse response = xhsCrawlerService.searchNotes(keyword, page,
						SearchSortType.valueOf(crawlerConfig.getSortType()));

				if (response == null || response.getData() == null || response.getData().getItems() == null
						|| response.getData().getItems().isEmpty()) {
					log.info("第{}页无更多数据，停止搜索", page);
					break;
				}

				// 提取有效的笔记项（包含xsecToken和xsecSource）
				List<SearchItem> pageNotes = xhsCrawlerService.extractValidNoteItems(response);

				allNotes.addAll(pageNotes);
				log.info("第{}页获取笔记数量: {}", page, pageNotes.size());

				// 检查是否达到最大数量
				if (allNotes.size() >= maxNotesPerKeyword) {
					allNotes = allNotes.subList(0, maxNotesPerKeyword);
					log.info("达到最大笔记数量限制: {}", maxNotesPerKeyword);
					break;
				}

				// 页面间隔
				if (page < maxPages) {
					sleep(crawlerConfig.getRequestInterval());
				}
			}
		}
		catch (Exception e) {
			log.error("搜索笔记失败: keyword={}", keyword, e);
		}

		return allNotes;
	}

	/**
	 * 去重处理 - 针对SearchItem
	 */
	private List<SearchItem> deduplicateSearchItems(List<SearchItem> searchItems) {
		return searchItems.stream()
			.filter(item -> item.getId() != null)
			.filter(item -> !dataStorageService.isNoteExists(item.getId()))
			.toList();
	}

	/**
	 * 睡眠等待
	 */
	private void sleep(long milliseconds) {
		try {
			TimeUnit.MILLISECONDS.sleep(milliseconds);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("睡眠被中断");
		}
	}

	/**
	 * 异步保存笔记信息
	 */
	@Async
	public CompletableFuture<Void> saveNoteAsync(NoteCard noteCard, String keyword, Long taskId) {
		try {
			dataStorageService.saveNoteInfos(List.of(noteCard), keyword, taskId);
			log.debug("异步保存笔记成功: noteId={}", noteCard.getNoteId());
		}
		catch (Exception e) {
			log.error("异步保存笔记失败: noteId={}, error={}", noteCard.getNoteId(), e.getMessage());
		}
		return CompletableFuture.completedFuture(null);
	}

	/**
	 * 异步保存用户信息
	 */
	@Async
	public CompletableFuture<Void> saveUserAsync(User user, Long taskId) {
		try {
			dataStorageService.saveUserInfo(user, taskId);
			log.debug("异步保存用户成功: userId={}", user.getUserId());
		}
		catch (Exception e) {
			log.error("异步保存用户失败: userId={}, error={}", user.getUserId(), e.getMessage());
		}
		return CompletableFuture.completedFuture(null);
	}

	/**
	 * 处理爬虫异常，转换为具体的异常类型 使用统一的异常处理工具类
	 */
	private RuntimeException handleCrawlerException(Exception e, String context) {
		exceptionHandler.handleTaskException(e, context);

		String message = Optional.ofNullable(e.getMessage()).orElse(e.getClass().getSimpleName());

		// 根据异常消息判断异常类型
		if (message.contains("IP") || message.contains("blocked") || message.contains("封禁")) {
			return new IpBlockedException(context + ": " + message, e);
		}
		else if (message.contains("rate limit") || message.contains("限流") || message.contains("too many requests")) {
			return new RateLimitException(context + ": " + message, e);
		}
		else if (message.contains("login") || message.contains("登录") || message.contains("unauthorized")) {
			return new LoginRequiredException(context + ": " + message, e);
		}
		else {
			return new DataFetchException(context + ": " + message, e);
		}
	}

	// ================== 新增的辅助方法 ==================

	/**
	 * 初始化任务
	 */
	private Long initializeTask(String taskName, String keyword) {
		Long taskId = dataStorageService.createCrawlTask(taskName, keyword);
		dataStorageService.startCrawlTask(taskId);
		crawlerStatusService.recordTaskStart(taskName, keyword);
		return taskId;
	}

	/**
	 * 搜索并准备搜索项
	 */
	private List<SearchItem> searchAndPrepareItems(String keyword, Long taskId) {
		log.info("步骤1: 搜索笔记");
		List<SearchItem> searchItems = searchNotes(keyword);

		// 去重处理
		if (crawlerConfig.getStorage().isEnableDeduplication()) {
			searchItems = deduplicateSearchItems(searchItems);
			log.info("去重后笔记数量: {}", searchItems.size());
		}

		// 设置任务信息
		searchItems.forEach(item -> {
			item.setTaskId(taskId);
			item.setKeyword(keyword);
		});

		return searchItems;
	}

	/**
	 * 处理笔记详情（边爬取边保存）
	 */
	private List<NoteCard> processNoteDetailsWithSave(List<SearchItem> searchItems, String keyword, Long taskId) {
		log.info("步骤2: 边爬取边保存笔记详情");

		return processBatch(searchItems, "笔记详情", item -> processNoteDetailWithSave(item, keyword, taskId));
	}

	/**
	 * 处理用户信息（边爬取边保存）
	 */
	private void processUserInfoWithSave(List<NoteCard> noteCards, Long taskId) {
		log.info("步骤3: 获取用户信息（边爬取边保存）");

		// 提取唯一的用户ID
		List<String> userIds = extractUniqueUserIds(noteCards);
		log.info("需要获取用户信息数量: {}", userIds.size());

		processBatch(userIds, "用户信息", userId -> processUserInfoWithSave(userId, taskId));

		log.info("批量获取用户信息完成（边爬取边保存）");
	}

	/**
	 * 通用批处理方法 - 减少重复代码
	 */
	private <T, R> List<R> processBatch(List<T> items, String batchType, Function<T, CompletableFuture<R>> processor) {
		List<R> results = new ArrayList<>();
		int batchSize = crawlerConfig.getConcurrency();

		for (int i = 0; i < items.size(); i += batchSize) {
			int endIndex = Math.min(i + batchSize, items.size());
			List<T> batch = items.subList(i, endIndex);

			log.info("处理{}批次: {}-{}/{}", batchType, i + 1, endIndex, items.size());

			// 并行处理当前批次
			List<CompletableFuture<R>> futures = batch.stream().map(processor).toList();

			// 等待当前批次完成
			List<R> batchResults = futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).toList();

			results.addAll(batchResults);

			// 批次间隔
			if (endIndex < items.size()) {
				sleep(crawlerConfig.getRequestInterval());
			}
		}

		log.info("批量获取{}完成（边爬取边保存）: success={}/{}", batchType, results.size(), items.size());
		return results;
	}

	/**
	 * 处理单个笔记详情（边爬取边保存）
	 */
	private CompletableFuture<NoteCard> processNoteDetailWithSave(SearchItem searchItem, String keyword, Long taskId) {
		return CompletableFuture.supplyAsync(() -> {
			return executeWithConcurrencyControl(searchItem.getId(), () -> {
				if (searchItem.getId() == null) {
					return null;
				}

				// 获取笔记详情
				String xsecSource = Optional.ofNullable(searchItem.getXsecSource()).orElse("pc_search");
				String xsecToken = Optional.ofNullable(searchItem.getXsecToken()).orElse("");

				Map<String, Object> noteDetailMap = retryTemplate.executeWithRetry(() -> {
					try {
						return xhsCrawlerService.getNoteDetail(searchItem.getId(), xsecSource, xsecToken);
					}
					catch (Exception e) {
						throw handleCrawlerException(e, "获取笔记详情失败: " + searchItem.getId());
					}
				});

				// 使用获取的详细数据更新NoteCard
				NoteCard noteCard = searchItem.getNoteCard();
				if (noteCard != null) {
					// 确保NoteCard有正确的noteId
					if (noteCard.getNoteId() == null && searchItem.getId() != null) {
						noteCard.setNoteId(searchItem.getId());
					}

					// 使用API返回的详细数据更新NoteCard
					if (noteDetailMap != null && !noteDetailMap.isEmpty()) {
						updateNoteCardWithDetailData(noteCard, noteDetailMap);
					}

					saveNoteAsync(noteCard, keyword, taskId);
				}

				return noteCard;
			}, () -> {
				// 失败时也尝试保存基础信息
				NoteCard noteCard = searchItem.getNoteCard();
				if (noteCard != null) {
					// 确保NoteCard有正确的noteId
					if (noteCard.getNoteId() == null && searchItem.getId() != null) {
						noteCard.setNoteId(searchItem.getId());
					}
					saveNoteAsync(noteCard, keyword, taskId);
				}
				return noteCard;
			});
		});
	}

	/**
	 * 处理单个用户信息（边爬取边保存）
	 */
	private CompletableFuture<User> processUserInfoWithSave(String userId, Long taskId) {
		return CompletableFuture.supplyAsync(() -> executeWithConcurrencyControl(userId, () -> {
			Map<String, Object> userInfo = retryTemplate.executeWithRetry(() -> {
				try {
					return xhsCrawlerService.getUserInfo(userId);
				}
				catch (Exception e) {
					throw handleCrawlerException(e, "获取用户信息失败: " + userId);
				}
			});

			// 将Map转换为User对象并立即异步保存
			User user = convertMapToUser(userInfo, userId);
			if (user != null) {
				saveUserAsync(user, taskId);
				return user;
			}

			return null;
		}, () -> null));
	}

	/**
	 * 通用的并发控制执行方法 - 减少重复代码
	 */
	private <T> T executeWithConcurrencyControl(String identifier, java.util.function.Supplier<T> successAction,
			java.util.function.Supplier<T> failureAction) {

		// 获取并发许可证
		if (!concurrencyControlService.acquirePermit(5000)) {
			log.warn("获取并发许可证超时: {}", identifier);
			concurrencyControlService.recordFailure();
			return failureAction.get();
		}

		try {
			T result = successAction.get();
			concurrencyControlService.recordSuccess();
			return result;
		}
		catch (Exception e) {
			concurrencyControlService.recordFailure();
			log.warn("处理失败: identifier={}, error={}", identifier, e.getMessage());
			return failureAction.get();
		}
		finally {
			concurrencyControlService.releasePermit();
		}
	}

	/**
	 * 检查响应是否为空
	 */
	private boolean isEmptyResponse(SearchResponse response) {
		return response == null || response.getData() == null || response.getData().getItems() == null
				|| response.getData().getItems().isEmpty();
	}

	/**
	 * 提取唯一的用户ID
	 */
	private List<String> extractUniqueUserIds(List<NoteCard> noteCards) {
		return noteCards.stream()
			.map(NoteCard::getUser)
			.filter(Objects::nonNull)
			.map(User::getUserId)
			.filter(Objects::nonNull)
			.distinct()
			.toList();
	}

	/**
	 * 完成任务处理
	 */
	private void completeTask(String taskName, TaskMetrics metrics, List<NoteCard> noteCards) {
		log.info("批量爬取任务完成: taskName={}, total={}, success={}, failed={}", taskName, metrics.totalNotes,
				metrics.successNotes, metrics.failedNotes);

		int userCount = crawlerConfig.isEnableUserInfo()
				? (int) noteCards.stream().map(NoteCard::getUser).filter(Objects::nonNull).count() : 0;

		crawlerStatusService.recordTaskComplete(taskName, metrics.successNotes, userCount, metrics.failedNotes);
	}

	/**
	 * 处理任务失败
	 */
	private String handleTaskFailure(Exception e, String taskName, String keyword, TaskMetrics metrics) {
		String context = String.format("批量爬取任务失败: taskName=%s, keyword=%s", taskName, keyword);
		exceptionHandler.handleTaskException(e, context);

		String errorMessage = e.getMessage();
		metrics.failedNotes = metrics.totalNotes - metrics.successNotes;

		crawlerStatusService.recordTaskFailure(taskName, errorMessage);
		return errorMessage;
	}

	/**
	 * 完成任务清理
	 */
	private void finalizeTask(Long taskId, TaskMetrics metrics, String errorMessage) {
		if (taskId != null) {
			dataStorageService.completeCrawlTask(taskId, metrics.totalNotes, metrics.successNotes, metrics.failedNotes,
					errorMessage);
		}
	}

	/**
	 * 将API返回的Map转换为User对象
	 */
	private User convertMapToUser(Map<String, Object> userInfo, String userId) {
		if (userInfo == null || userInfo.isEmpty()) {
			return null;
		}

		try {
			User user = new User();
			user.setUserId(userId);

			// 基本信息
			user.setNickname(getStringValue(userInfo, "nickname"));
			user.setDesc(getStringValue(userInfo, "desc"));

			// 处理头像信息 - images字段可能是字符串或List
			Object images = userInfo.get("images");
			if (images instanceof String) {
				user.setAvatar((String) images);
			}
			else if (images instanceof java.util.List) {
				java.util.List<?> imageList = (java.util.List<?>) images;
				if (!imageList.isEmpty() && imageList.get(0) instanceof String) {
					user.setAvatar((String) imageList.get(0));
				}
			}

			// 统计信息 - 从API的standardizeUserInfo方法返回的字段
			user.setFollowingCount(getIntegerValue(userInfo, "follows"));
			user.setFollowersCount(getIntegerValue(userInfo, "fans"));
			user.setLikeCount(getIntegerValue(userInfo, "interaction"));

			// 处理标签信息
			Object tagList = userInfo.get("tagList");
			if (tagList instanceof java.util.Map) {
				java.util.Map<String, String> tagMap = (java.util.Map<String, String>) tagList;
				user.setTags(new java.util.ArrayList<>(tagMap.values()));
			}

			log.debug("成功转换用户信息: userId={}, nickname={}", userId, user.getNickname());
			return user;
		}
		catch (Exception e) {
			log.error("转换用户信息失败: userId={}, error={}", userId, e.getMessage());
			return null;
		}
	}

	/**
	 * 安全地从Map中获取字符串值
	 */
	private String getStringValue(Map<String, Object> map, String key) {
		Object value = map.get(key);
		return value != null ? value.toString() : null;
	}

	/**
	 * 安全地从Map中获取整数值
	 */
	private Integer getIntegerValue(Map<String, Object> map, String key) {
		Object value = map.get(key);
		if (value == null) {
			return null;
		}
		if (value instanceof Integer) {
			return (Integer) value;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		try {
			return Integer.parseInt(value.toString());
		}
		catch (NumberFormatException e) {
			log.warn("无法转换为整数: key={}, value={}", key, value);
			return null;
		}
	}

	/**
	 * 使用API返回的详细数据更新NoteCard
	 */
	private void updateNoteCardWithDetailData(NoteCard noteCard, Map<String, Object> noteDetailMap) {
		try {
			if (noteDetailMap == null || noteDetailMap.isEmpty()) {
				return;
			}

			// 更新标题信息
			if (noteDetailMap.containsKey("title") && noteDetailMap.get("title") != null) {
				noteCard.setTitle(noteDetailMap.get("title").toString());
			}

			if (noteDetailMap.containsKey("display_title") && noteDetailMap.get("display_title") != null) {
				noteCard.setDisplayTitle(noteDetailMap.get("display_title").toString());
			}

			// 更新笔记类型
			if (noteDetailMap.containsKey("type") && noteDetailMap.get("type") != null) {
				noteCard.setType(noteDetailMap.get("type").toString());
			}

			// 更新图片列表
			if (noteDetailMap.containsKey("image_list") && noteDetailMap.get("image_list") instanceof List) {
				List<Map<String, Object>> imageListData = (List<Map<String, Object>>) noteDetailMap.get("image_list");
				List<ImageInfo> imageInfoList = imageListData.stream()
					.map(this::convertMapToImageInfo)
					.filter(Objects::nonNull)
					.toList();
				noteCard.setImageList(imageInfoList);
			}

			// 更新封面信息
			if (noteDetailMap.containsKey("cover") && noteDetailMap.get("cover") instanceof Map) {
				Map<String, Object> coverData = (Map<String, Object>) noteDetailMap.get("cover");
				Cover cover = convertMapToCover(coverData);
				if (cover != null) {
					noteCard.setCover(cover);
				}
			}

			// 更新互动信息
			if (noteDetailMap.containsKey("interact_info") && noteDetailMap.get("interact_info") instanceof Map) {
				Map<String, Object> interactData = (Map<String, Object>) noteDetailMap.get("interact_info");
				InteractInfo interactInfo = convertMapToInteractInfo(interactData);
				if (interactInfo != null) {
					noteCard.setInteractInfo(interactInfo);
				}
			}

			// 更新用户信息
			if (noteDetailMap.containsKey("user") && noteDetailMap.get("user") instanceof Map) {
				Map<String, Object> userData = (Map<String, Object>) noteDetailMap.get("user");
				User user = convertMapToUserBasic(userData);
				if (user != null) {
					noteCard.setUser(user);
				}
			}

			// 更新标签列表
			if (noteDetailMap.containsKey("tag_list") && noteDetailMap.get("tag_list") instanceof List) {
				List<Object> tagListData = (List<Object>) noteDetailMap.get("tag_list");
				List<String> tagList = tagListData.stream().filter(Objects::nonNull).map(Object::toString).toList();
				noteCard.setTagList(tagList);
			}

			log.debug("成功更新NoteCard详细信息: noteId={}", noteCard.getNoteId());
		}
		catch (Exception e) {
			log.warn("更新NoteCard详细信息失败: noteId={}, error={}", noteCard.getNoteId(), e.getMessage());
		}
	}

	/**
	 * 将Map转换为ImageInfo对象
	 */
	private ImageInfo convertMapToImageInfo(Map<String, Object> imageData) {
		if (imageData == null || imageData.isEmpty()) {
			return null;
		}

		try {
			ImageInfo imageInfo = new ImageInfo();

			// 优先使用url_size_large，否则使用url_default，最后使用url
			if (imageData.containsKey("url_size_large")) {
				imageInfo.setUrl(getStringValue(imageData, "url_size_large"));
			}
			else if (imageData.containsKey("url_default")) {
				imageInfo.setUrl(getStringValue(imageData, "url_default"));
			}
			else if (imageData.containsKey("url")) {
				imageInfo.setUrl(getStringValue(imageData, "url"));
			}

			if (imageData.containsKey("width")) {
				imageInfo.setWidth(getIntegerValue(imageData, "width"));
			}
			if (imageData.containsKey("height")) {
				imageInfo.setHeight(getIntegerValue(imageData, "height"));
			}

			return imageInfo;
		}
		catch (Exception e) {
			log.warn("转换ImageInfo失败: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * 将Map转换为Cover对象
	 */
	private Cover convertMapToCover(Map<String, Object> coverData) {
		if (coverData == null || coverData.isEmpty()) {
			return null;
		}

		try {
			Cover cover = new Cover();

			// 优先使用url_size_large，否则使用url_default，最后使用url
			if (coverData.containsKey("url_size_large")) {
				cover.setUrl(getStringValue(coverData, "url_size_large"));
			}
			else if (coverData.containsKey("url_default")) {
				cover.setUrl(getStringValue(coverData, "url_default"));
			}
			else if (coverData.containsKey("url")) {
				cover.setUrl(getStringValue(coverData, "url"));
			}

			if (coverData.containsKey("width")) {
				cover.setWidth(getIntegerValue(coverData, "width"));
			}
			if (coverData.containsKey("height")) {
				cover.setHeight(getIntegerValue(coverData, "height"));
			}

			return cover;
		}
		catch (Exception e) {
			log.warn("转换Cover失败: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * 将Map转换为InteractInfo对象
	 */
	private InteractInfo convertMapToInteractInfo(Map<String, Object> interactData) {
		if (interactData == null || interactData.isEmpty()) {
			return null;
		}

		try {
			InteractInfo interactInfo = new InteractInfo();

			if (interactData.containsKey("liked_count")) {
				interactInfo.setLikedCount(getStringValue(interactData, "liked_count"));
			}
			if (interactData.containsKey("collected_count")) {
				interactInfo.setCollectedCount(getStringValue(interactData, "collected_count"));
			}
			if (interactData.containsKey("comment_count")) {
				interactInfo.setCommentCount(getStringValue(interactData, "comment_count"));
			}
			if (interactData.containsKey("shared_count")) {
				interactInfo.setSharedCount(getStringValue(interactData, "shared_count"));
			}

			return interactInfo;
		}
		catch (Exception e) {
			log.warn("转换InteractInfo失败: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * 将Map转换为User对象（基础版本，用于NoteCard中的用户信息）
	 */
	private User convertMapToUserBasic(Map<String, Object> userData) {
		if (userData == null || userData.isEmpty()) {
			return null;
		}

		try {
			User user = new User();

			if (userData.containsKey("user_id")) {
				user.setUserId(getStringValue(userData, "user_id"));
			}
			if (userData.containsKey("nickname")) {
				user.setNickname(getStringValue(userData, "nickname"));
			}
			if (userData.containsKey("avatar")) {
				user.setAvatar(getStringValue(userData, "avatar"));
			}

			// 处理头像信息 - images字段可能是字符串或List
			Object images = userData.get("images");
			if (images instanceof String) {
				user.setAvatar((String) images);
			}
			else if (images instanceof List) {
				List<?> imageList = (List<?>) images;
				if (!imageList.isEmpty() && imageList.get(0) instanceof String) {
					user.setAvatar((String) imageList.get(0));
				}
			}

			return user;
		}
		catch (Exception e) {
			log.warn("转换User失败: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * 任务指标内部类
	 */
	private static class TaskMetrics {

		int totalNotes = 0;

		int successNotes = 0;

		int failedNotes = 0;

	}

}