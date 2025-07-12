package com.libre.spider;

import com.libre.spider.service.XhsCrawlerService;
import com.libre.spider.enums.SearchSortType;
import com.libre.spider.model.SearchResponse;
import com.libre.spider.model.SearchItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

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
			SearchResponse response = xhsCrawlerService.searchNotes("美食", 1, SearchSortType.GENERAL);

			if (response != null && response.getSuccess() != null && response.getSuccess()) {
				System.out.println("✓ 搜索成功");
				System.out.println("- 响应消息: " + response.getMsg());
				System.out.println("- 响应代码: " + response.getCode());

				if (response.getData() != null) {
					System.out.println("- 是否有更多: " + response.getData().getHasMore());

					// 获取笔记列表
					List<SearchItem> items = response.getData().getItems();

					if (items != null && !items.isEmpty()) {
						System.out.println("- 找到 " + items.size() + " 条笔记");

						// 打印第一条笔记的信息
						SearchItem firstNote = items.get(0);
						System.out.println("\n第一条笔记信息:");
						System.out.println("- ID: " + firstNote.getId());
						System.out.println("- 模型类型: " + firstNote.getModelType());
						System.out.println("- xsec_token: " + firstNote.getXsecToken());

						if (firstNote.getNoteCard() != null) {
							System.out.println("- 标题: " + firstNote.getNoteCard().getDisplayTitle());
							System.out.println("- 类型: " + firstNote.getNoteCard().getType());

							if (firstNote.getNoteCard().getInteractInfo() != null) {
								System.out
									.println("- 点赞数: " + firstNote.getNoteCard().getInteractInfo().getLikedCount());
								System.out
									.println("- 评论数: " + firstNote.getNoteCard().getInteractInfo().getCommentCount());
							}

							if (firstNote.getNoteCard().getUser() != null) {
								System.out.println("- 作者: " + firstNote.getNoteCard().getUser().getNickname());
								System.out.println("- 作者ID: " + firstNote.getNoteCard().getUser().getUserId());
							}
						}
					}
					else {
						System.out.println("× 未找到笔记数据");
					}
				}
				else {
					System.out.println("× 搜索数据为空");
				}
			}
			else {
				System.out.println("× 搜索失败");
				if (response != null) {
					System.out.println("- 错误消息: " + response.getMsg());
					System.out.println("- 错误代码: " + response.getCode());
				}
			}
		}
		catch (Exception e) {
			System.err.println("× 搜索失败: " + e.getMessage());
			e.printStackTrace();
		}
	}

}