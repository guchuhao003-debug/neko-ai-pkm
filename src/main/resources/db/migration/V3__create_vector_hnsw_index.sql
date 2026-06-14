-- V3：pgvector HNSW 索引，单独执行
-- 使用 HNSW + 余弦距离索引加速语义检索。
create index if not exists idx_vector_store_embedding
    on vector_store using hnsw (embedding vector_cosine_ops);

-- 查询最近一周新增或修改的笔记。
create index if not exists idx_note_updated_at
    on note (updated_at desc);

-- 查询未向量化或近期变更的笔记。
create index if not exists idx_note_vectorized_updated
    on note (vectorized, updated_at desc);

-- 按源笔记查询推荐链接。
create index if not exists idx_note_link_source
    on note_link (source_note_id);

-- 按目标笔记反查引用关系。
create index if not exists idx_note_link_target
    on note_link (target_note_id);

-- 根据 note_id 删除旧向量块时会用到这个表达式索引。
create index if not exists idx_vector_store_note_id
    on vector_store ((metadata ->> 'note_id'));