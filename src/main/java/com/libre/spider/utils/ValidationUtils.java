package com.libre.spider.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 参数验证工具类 提供统一的参数验证逻辑，避免重复的验证代码
 */
@Slf4j
@Component
public class ValidationUtils {

	private static final Pattern URL_PATTERN = Pattern
		.compile("^(https?://)?(www\\.)?xiaohongshu\\.com/(explore|user|discovery)/.*");

	private static final Pattern NOTE_ID_PATTERN = Pattern.compile("^[0-9a-fA-F]{24}$");

	private static final Pattern USER_ID_PATTERN = Pattern.compile("^[0-9a-fA-F]{24}$");

	/**
	 * 验证字符串是否为空
	 * @param value 待验证的值
	 * @param fieldName 字段名称
	 * @return 验证结果Map
	 */
	public Map<String, Object> validateNotEmpty(String value, String fieldName) {
		if (!StringUtils.hasText(value)) {
			String error = String.format("%s不能为空", fieldName);
			log.warn("参数验证失败: {}", error);
			return Map.of("success", false, "error", error);
		}
		return Map.of("success", true);
	}

	/**
	 * 验证小红书笔记URL格式
	 * @param noteUrl 笔记URL
	 * @return 验证结果Map
	 */
	public Map<String, Object> validateNoteUrl(String noteUrl) {
		Map<String, Object> emptyCheck = validateNotEmpty(noteUrl, "笔记URL");
		if (!(Boolean) emptyCheck.get("success")) {
			return emptyCheck;
		}

		if (!URL_PATTERN.matcher(noteUrl.trim()).find()) {
			String error = "无效的小红书笔记URL格式";
			log.warn("URL格式验证失败: {}", noteUrl);
			return Map.of("success", false, "error", error);
		}

		return Map.of("success", true);
	}

	/**
	 * 验证笔记ID格式
	 * @param noteId 笔记ID
	 * @return 验证结果Map
	 */
	public Map<String, Object> validateNoteId(String noteId) {
		Map<String, Object> emptyCheck = validateNotEmpty(noteId, "笔记ID");
		if (!(Boolean) emptyCheck.get("success")) {
			return emptyCheck;
		}

		if (!NOTE_ID_PATTERN.matcher(noteId.trim()).matches()) {
			String error = "无效的笔记ID格式，应为24位十六进制字符串";
			log.warn("笔记ID格式验证失败: {}", noteId);
			return Map.of("success", false, "error", error);
		}

		return Map.of("success", true);
	}

	/**
	 * 验证用户ID格式
	 * @param userId 用户ID
	 * @return 验证结果Map
	 */
	public Map<String, Object> validateUserId(String userId) {
		Map<String, Object> emptyCheck = validateNotEmpty(userId, "用户ID");
		if (!(Boolean) emptyCheck.get("success")) {
			return emptyCheck;
		}

		if (!USER_ID_PATTERN.matcher(userId.trim()).matches()) {
			String error = "无效的用户ID格式，应为24位十六进制字符串";
			log.warn("用户ID格式验证失败: {}", userId);
			return Map.of("success", false, "error", error);
		}

		return Map.of("success", true);
	}

	/**
	 * 验证搜索关键词
	 * @param keyword 搜索关键词
	 * @return 验证结果Map
	 */
	public Map<String, Object> validateKeyword(String keyword) {
		Map<String, Object> emptyCheck = validateNotEmpty(keyword, "搜索关键词");
		if (!(Boolean) emptyCheck.get("success")) {
			return emptyCheck;
		}

		String trimmedKeyword = keyword.trim();
		if (trimmedKeyword.length() > 100) {
			String error = "搜索关键词长度不能超过100个字符";
			log.warn("关键词长度验证失败: {} (长度: {})", keyword, trimmedKeyword.length());
			return Map.of("success", false, "error", error);
		}

		return Map.of("success", true);
	}

	/**
	 * 验证页码参数
	 * @param page 页码
	 * @return 验证结果Map
	 */
	public Map<String, Object> validatePage(Integer page) {
		if (page == null || page < 1) {
			String error = "页码必须大于0";
			log.warn("页码验证失败: {}", page);
			return Map.of("success", false, "error", error);
		}

		if (page > 1000) {
			String error = "页码不能超过1000";
			log.warn("页码验证失败: {}", page);
			return Map.of("success", false, "error", error);
		}

		return Map.of("success", true);
	}

	/**
	 * 验证任务名称
	 * @param taskName 任务名称
	 * @return 验证结果Map
	 */
	public Map<String, Object> validateTaskName(String taskName) {
		Map<String, Object> emptyCheck = validateNotEmpty(taskName, "任务名称");
		if (!(Boolean) emptyCheck.get("success")) {
			return emptyCheck;
		}

		String trimmedTaskName = taskName.trim();
		if (trimmedTaskName.length() > 50) {
			String error = "任务名称长度不能超过50个字符";
			log.warn("任务名称长度验证失败: {} (长度: {})", taskName, trimmedTaskName.length());
			return Map.of("success", false, "error", error);
		}

		// 检查是否包含特殊字符
		if (!trimmedTaskName.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9_\\-\\s]+$")) {
			String error = "任务名称只能包含中文、英文、数字、下划线、横线和空格";
			log.warn("任务名称格式验证失败: {}", taskName);
			return Map.of("success", false, "error", error);
		}

		return Map.of("success", true);
	}

	/**
	 * 验证多个参数 返回第一个验证失败的结果，如果都通过则返回成功
	 * @param validations 验证结果列表
	 * @return 综合验证结果
	 */
	@SafeVarargs
	public final Map<String, Object> validateAll(Map<String, Object>... validations) {
		for (Map<String, Object> validation : validations) {
			if (!(Boolean) validation.get("success")) {
				return validation;
			}
		}
		return Map.of("success", true);
	}

	/**
	 * 快速验证字符串参数
	 * @param value 参数值
	 * @param fieldName 字段名称
	 * @throws IllegalArgumentException 参数验证失败时抛出
	 */
	public void requireNonEmpty(String value, String fieldName) {
		if (!StringUtils.hasText(value)) {
			String error = String.format("%s不能为空", fieldName);
			log.error("参数验证失败: {}", error);
			throw new IllegalArgumentException(error);
		}
	}

	/**
	 * 快速验证对象参数
	 * @param value 参数值
	 * @param fieldName 字段名称
	 * @throws IllegalArgumentException 参数验证失败时抛出
	 */
	public void requireNonNull(Object value, String fieldName) {
		if (value == null) {
			String error = String.format("%s不能为空", fieldName);
			log.error("参数验证失败: {}", error);
			throw new IllegalArgumentException(error);
		}
	}

	/**
	 * 验证数值范围
	 * @param value 数值
	 * @param min 最小值
	 * @param max 最大值
	 * @param fieldName 字段名称
	 * @return 验证结果Map
	 */
	public Map<String, Object> validateRange(Integer value, int min, int max, String fieldName) {
		if (value == null) {
			String error = String.format("%s不能为空", fieldName);
			return Map.of("success", false, "error", error);
		}

		if (value < min || value > max) {
			String error = String.format("%s必须在%d到%d之间", fieldName, min, max);
			log.warn("数值范围验证失败: {} = {}", fieldName, value);
			return Map.of("success", false, "error", error);
		}

		return Map.of("success", true);
	}

}