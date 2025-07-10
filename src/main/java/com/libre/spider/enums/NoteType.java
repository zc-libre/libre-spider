package com.libre.spider.enums;

/**
 * 笔记类型
 */
public enum NoteType {

	/**
	 * 普通笔记
	 */
	NORMAL("normal"),

	/**
	 * 视频笔记
	 */
	VIDEO("video");

	private final String value;

	NoteType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}