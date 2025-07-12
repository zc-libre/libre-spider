package com.libre.spider.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.libre.spider.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户信息Mapper接口
 */
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {

}