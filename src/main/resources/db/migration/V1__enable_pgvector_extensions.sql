-- V1：扩展初始化，单独执行

-- 启用 pgvector 扩展，用于 vector 类型和向量索引。
create extension if not exists vector;

-- 启用 uuid 扩展，用于生成 vector_store 主键。
create extension if not exists "uuid-ossp";