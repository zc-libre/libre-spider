package com.libre.spider;

import com.libre.spider.service.XhsCrawlerService;
import com.libre.spider.enums.SearchSortType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

/**
 * 搜索功能测试
 */
@SpringBootTest
@ActiveProfiles("dev")
public class SearchTest {

	@Autowired
	private XhsCrawlerService xhsCrawlerService;

	@Test
	public void testSearchNotes() throws Exception {
		System.out.println("=== 测试搜索笔记功能 ===");

		try {
			// 搜索美食相关的笔记
			Map<String, Object> response = xhsCrawlerService.searchNotes("美食", 1, SearchSortType.GENERAL);

			if (response != null && response.get("has_more") != null) {
				System.out.println("✓ 搜索成功");
				System.out.println("- 是否有更多: " + response.get("has_more"));

				// 获取笔记列表
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

				if (items != null && !items.isEmpty()) {
					System.out.println("- 找到 " + items.size() + " 条笔记");

					// 打印第一条笔记的信息
					Map<String, Object> firstNote = items.get(0);
					System.out.println("\n第一条笔记信息:");
					System.out.println("- ID: " + firstNote.get("id"));
					System.out.println("- 标题: " + firstNote.get("display_title"));

					@SuppressWarnings("unchecked")
					Map<String, Object> noteCard = (Map<String, Object>) firstNote.get("note_card");
					if (noteCard != null) {
						System.out.println("- 类型: " + noteCard.get("type"));

						@SuppressWarnings("unchecked")
						Map<String, Object> interactInfo = (Map<String, Object>) noteCard.get("interact_info");
						if (interactInfo != null) {
							System.out.println("- 点赞数: " + interactInfo.get("liked_count"));
						}

						@SuppressWarnings("unchecked")
						Map<String, Object> user = (Map<String, Object>) noteCard.get("user");
						if (user != null) {
							System.out.println("- 作者: " + user.get("nickname"));
						}
					}
				}
				else {
					System.out.println("× 未找到笔记数据");
				}
			}
			else {
				System.out.println("× 搜索响应无效");
			}
		}
		catch (Exception e) {
			System.err.println("× 搜索失败: " + e.getMessage());
			e.printStackTrace();
		}
	}

}