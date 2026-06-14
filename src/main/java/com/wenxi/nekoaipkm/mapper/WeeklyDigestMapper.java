package com.wenxi.nekoaipkm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenxi.nekoaipkm.model.entity.WeeklyDigest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 周报表 Mapper，负责保存每周知识摘要
 */
@Mapper
public interface WeeklyDigestMapper extends BaseMapper<WeeklyDigest> {
}
