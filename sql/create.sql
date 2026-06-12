-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS neko_ai_pkm
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE neko_ai_pkm;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userEmail    varchar(256)                           null comment '用户邮箱',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userEmail (userEmail),
    INDEX idx_userName (userName)
    ) comment '用户' collate = utf8mb4_unicode_ci;


-- 笔记主表
CREATE TABLE note (
                      id VARCHAR(64) PRIMARY KEY COMMENT '笔记UUID',
                      title VARCHAR(500) NOT NULL COMMENT '笔记标题',
                      sourceType VARCHAR(20) NOT NULL COMMENT '来源: obsidian/markdown/pdf/web',
                      sourcePath VARCHAR(1000) COMMENT '源文件路径或URL',
                      createdAt DATETIME NOT NULL COMMENT '笔记创建时间',
                      updatedAt DATETIME NOT NULL COMMENT '最后更新时间',
                      wordCount INT DEFAULT 0,
                      tags VARCHAR(500) COMMENT '标签，逗号分隔',
                      isVectorized BOOLEAN DEFAULT FALSE COMMENT '是否已向量化',
                      UNIQUE KEY uk_source_path (sourcePath(255))
);

-- 笔记关联表（双向链接）
CREATE TABLE note_link (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           sourceNoteId VARCHAR(64) NOT NULL COMMENT '源笔记ID',
                           targetNoteId VARCHAR(64) NOT NULL COMMENT '目标笔记ID',
                           confidence FLOAT DEFAULT 0.0 COMMENT '关联置信度',
                           createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
                           INDEX idx_source (sourceNoteId),
                           INDEX idx_target (targetNoteId),
                           UNIQUE KEY uk_link (sourceNoteId, targetNoteId)
);

-- 周报记录表
CREATE TABLE weekly_digest (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               weekStart DATE NOT NULL COMMENT '周开始日期',
                               content TEXT COMMENT '摘要内容',
                               sentAt DATETIME COMMENT '发送时间',
                               sentStatus VARCHAR(20) DEFAULT 'pending'
);

-- 查询场景
-- “最近一周新增/修改的笔记”（WHERE updated_at > DATE_SUB(NOW(), INTERVAL 7 DAY) ORDER BY updated_at DESC）
-- 定时任务扫描最近变更的笔记进行增量向量化
ALTER TABLE note ADD INDEX idx_updated_at (updatedAt);

-- 查询场景
-- 定时任务扫描未向量化的笔记进行导入（WHERE is_vectorized = FALSE）
-- 增量向量化任务
-- 如果经常结合 updated_at 一起使用
ALTER TABLE note ADD INDEX idx_vectorized_updated (isVectorized, updatedAt);

-- 以下暂不使用

-- 查询场景
-- 周报生成：“查询本周新增的笔记”（WHERE created_at BETWEEN ...）
-- 时间线展示
ALTER TABLE note ADD INDEX idx_created_at (createdAt);

-- 查询场景
-- “只检索 Obsidian 笔记”（WHERE source_type = 'obsidian'）
-- 批量重导入某种来源的笔记
ALTER TABLE note ADD INDEX idx_source_type (sourceType);
