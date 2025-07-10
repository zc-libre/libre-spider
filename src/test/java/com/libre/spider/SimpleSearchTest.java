package com.libre.spider;

import com.libre.spider.utils.XhsSignatureHelper;
import org.junit.jupiter.api.Test;

/**
 * 简单的搜索测试 - 不使用Spring和Playwright
 */
public class SimpleSearchTest {

	@Test
	public void testSignature() {
		System.out.println("=== 测试签名生成 ===");

		XhsSignatureHelper helper = new XhsSignatureHelper();

		// 测试数据
		String a1 = "test_a1";
		String b1 = "test_b1";
		String xS = "XYZ_test";
		String xT = "1234567890";

		// 生成签名
		var result = helper.sign(a1, b1, xS, xT);

		System.out.println("签名结果:");
		result.forEach((k, v) -> System.out.println(k + ": " + v));

		// 测试搜索ID生成
		String searchId = helper.getSearchId();
		System.out.println("\n搜索ID: " + searchId);
		System.out.println("搜索ID长度: " + searchId.length());
	}

}