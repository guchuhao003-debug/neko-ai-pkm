package com.wenxi.nekoaipkm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenxi.nekoaipkm.model.entity.NoteLink;
import org.apache.ibatis.annotations.Mapper;

/**
 * 笔记关联表 Mapper, 负责保存语义链接结果。
 */
@Mapper
public interface NoteLinkMapper extends BaseMapper<NoteLink> {
}
