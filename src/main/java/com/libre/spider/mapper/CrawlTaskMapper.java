package com.libre.spider.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.libre.spider.entity.CrawlTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 爬取任务Mapper接口
 */
@Mapper
public interface CrawlTaskMapper extends BaseMapper<CrawlTask> {

}