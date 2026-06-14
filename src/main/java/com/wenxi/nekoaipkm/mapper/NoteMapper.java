package com.wenxi.nekoaipkm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenxi.nekoaipkm.model.entity.Note;
import org.apache.ibatis.annotations.Mapper;

/**
 * 笔记表 Mapper, 基础 CRUD （ MyBatis-Plus 支持）
 */
@Mapper
public interface NoteMapper extends BaseMapper<Note> {
}
