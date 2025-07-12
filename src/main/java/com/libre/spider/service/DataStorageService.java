package com.libre.spider.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libre.spider.entity.NoteInfo;
import com.libre.spider.entity.UserInfo;
import com.libre.spider.model.NoteCard;
import com.libre.spider.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据存储服务 负责协调各个数据访问服务，处理业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataStorageService {

	private final TaskDataService taskDataService;

	private final NoteDataService noteDataService;

	private final UserDataService userDataService;

	private final ObjectMapper objectMapper;

	/**
	 * 创建爬取任务
	 * @param taskName 任务名称
	 * @param keyword 关键词
	 * @return 任务ID
	 */
	public Long createCrawlTask(String taskName, String keyword) {
		return taskDataService.createTask(taskName, keyword);
	}

	/**
	 * 启动爬取任务
	 * @param taskId 任务ID
	 */
	public void startCrawlTask(Long taskId) {
		taskDataService.startTask(taskId);
	}

	/**
	 * 完成爬取任务
	 * @param taskId 任务ID
	 * @param totalNotes 总笔记数
	 * @param successNotes 成功笔记数
	 * @param failedNotes 失败笔记数
	 * @param errorMessage 错误信息
	 */
	public void completeCrawlTask(Long taskId, int totalNotes, int successNotes, int failedNotes, String errorMessage) {
		taskDataService.completeTask(taskId, totalNotes, successNotes, failedNotes, errorMessage);
	}

	/**
	 * 批量保存笔记信息
	 * @param noteCards 笔记卡片列表
	 * @param keyword 关键词
	 * @param taskId 任务ID
	 */
	@Transactional
	public void saveNoteInfos(List<NoteCard> noteCards, String keyword, Long taskId) {
		if (noteCards == null || noteCards.isEmpty()) {
			return;
		}

		List<NoteInfo> noteInfos = noteCards.stream()
			.map(noteCard -> convertToNoteInfo(noteCard, keyword, taskId))
			.collect(Collectors.toList());

		// 使用数据访问服务批量保存
		noteDataService.batchSaveOrUpdate(noteInfos, noteInfo -> {
			com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NoteInfo> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
			wrapper.eq(NoteInfo::getNoteId, noteInfo.getNoteId());
			return wrapper;
		});

		log.info("批量保存笔记信息完成: count={}, keyword={}, taskId={}", noteInfos.size(), keyword, taskId);
	}

	/**
	 * 保存用户信息
	 * @param user 用户对象
	 * @param taskId 任务ID
	 */
	@Transactional
	public void saveUserInfo(User user, Long taskId) {
		if (user == null) {
			return;
		}

		UserInfo userInfo = convertToUserInfo(user, taskId);
		userDataService.saveOrUpdateByUserId(userInfo);

		log.info("保存用户信息完成: userId={}, nickname={}, taskId={}", user.getUserId(), user.getNickname(), taskId);
	}

	/**
	 * 转换NoteCard为NoteInfo
	 */
	private NoteInfo convertToNoteInfo(NoteCard noteCard, String keyword, Long taskId) {
		NoteInfo noteInfo = new NoteInfo();
		noteInfo.setNoteId(noteCard.getNoteId());
		noteInfo.setTitle(noteCard.getTitle());
		noteInfo.setNoteType(noteCard.getType());
		noteInfo.setKeyword(keyword);
		noteInfo.setTaskId(taskId);

		// 处理用户信息
		if (noteCard.getUser() != null) {
			noteInfo.setUserId(noteCard.getUser().getUserId());
			noteInfo.setUserName(noteCard.getUser().getNickname());
			noteInfo.setUserAvatar(noteCard.getUser().getAvatar());
		}

		// 处理封面信息
		if (noteCard.getCover() != null) {
			noteInfo.setCoverUrl(noteCard.getCover().getUrl());
		}

		// 处理交互信息
		if (noteCard.getInteractInfo() != null) {
			noteInfo.setLikeCount(parseIntegerSafely(noteCard.getInteractInfo().getLikedCount()));
			noteInfo.setCommentCount(parseIntegerSafely(noteCard.getInteractInfo().getCommentCount()));
			noteInfo.setCollectCount(parseIntegerSafely(noteCard.getInteractInfo().getCollectedCount()));
			noteInfo.setShareCount(parseIntegerSafely(noteCard.getInteractInfo().getSharedCount()));
		}

		// 处理图片URLs
		if (noteCard.getImageList() != null && !noteCard.getImageList().isEmpty()) {
			try {
				List<String> imageUrls = noteCard.getImageList()
					.stream()
					.map(img -> img.getUrl())
					.collect(Collectors.toList());
				noteInfo.setImageUrls(objectMapper.writeValueAsString(imageUrls));
			}
			catch (Exception e) {
				log.warn("转换图片URLs失败: {}", e.getMessage());
			}
		}

		// 生成笔记URL
		if (noteCard.getNoteId() != null) {
			noteInfo.setNoteUrl("https://www.xiaohongshu.com/explore/" + noteCard.getNoteId());
		}

		return noteInfo;
	}

	/**
	 * 转换User为UserInfo
	 */
	private UserInfo convertToUserInfo(User user, Long taskId) {
		UserInfo userInfo = new UserInfo();
		userInfo.setUserId(user.getUserId());
		userInfo.setNickname(user.getNickname());
		userInfo.setAvatar(user.getAvatar());
		userInfo.setDescription(user.getDesc());
		userInfo.setTaskId(taskId);

		// 处理统计信息
		if (user.getFollowersCount() != null) {
			userInfo.setFollowersCount(user.getFollowersCount());
		}
		if (user.getFollowingCount() != null) {
			userInfo.setFollowingCount(user.getFollowingCount());
		}
		if (user.getLikeCount() != null) {
			userInfo.setLikeCount(user.getLikeCount());
		}
		if (user.getNoteCount() != null) {
			userInfo.setNoteCount(user.getNoteCount());
		}

		// 处理标签信息
		if (user.getTags() != null && !user.getTags().isEmpty()) {
			try {
				userInfo.setTags(objectMapper.writeValueAsString(user.getTags()));
			}
			catch (Exception e) {
				log.warn("转换用户标签失败: {}", e.getMessage());
			}
		}

		// 生成用户主页URL
		if (user.getUserId() != null) {
			userInfo.setProfileUrl("https://www.xiaohongshu.com/user/profile/" + user.getUserId());
		}

		return userInfo;
	}

	/**
	 * 检查笔记是否已存在
	 * @param noteId 笔记ID
	 * @return 是否存在
	 */
	public boolean isNoteExists(String noteId) {
		return noteDataService.existsByNoteId(noteId);
	}

	/**
	 * 检查用户是否已存在
	 * @param userId 用户ID
	 * @return 是否存在
	 */
	public boolean isUserExists(String userId) {
		return userDataService.existsByUserId(userId);
	}

	/**
	 * 安全地将字符串转换为整数
	 */
	private Integer parseIntegerSafely(String value) {
		if (value == null || value.trim().isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(value.trim());
		}
		catch (NumberFormatException e) {
			log.warn("字符串转整数失败: {}", value);
			return 0;
		}
	}

}