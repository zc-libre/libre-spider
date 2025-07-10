package com.libre.spider.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Cookie工具类
 */
public class CookieUtils {

	/**
	 * 将Cookie字符串转换为Map
	 * @param cookieStr Cookie字符串，格式如: "a1=xxx; web_session=yyy"
	 * @return Cookie Map
	 */
	public static Map<String, String> convertStrCookieToMap(String cookieStr) {
		Map<String, String> cookieMap = new HashMap<>();

		if (cookieStr == null || cookieStr.trim().isEmpty()) {
			return cookieMap;
		}

		String[] cookies = cookieStr.split(";");
		for (String cookie : cookies) {
			String[] parts = cookie.trim().split("=", 2);
			if (parts.length == 2) {
				cookieMap.put(parts[0].trim(), parts[1].trim());
			}
		}

		return cookieMap;
	}

	/**
	 * 将Cookie Map转换为字符串
	 * @param cookieMap Cookie Map
	 * @return Cookie字符串
	 */
	public static String convertMapToCookieStr(Map<String, String> cookieMap) {
		if (cookieMap == null || cookieMap.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		cookieMap.forEach((key, value) -> {
			if (sb.length() > 0) {
				sb.append("; ");
			}
			sb.append(key).append("=").append(value);
		});

		return sb.toString();
	}

}