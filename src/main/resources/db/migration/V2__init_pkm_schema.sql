-- V2：普通表结构，事务内执行

-- 笔记主表，保存文件级别的元数据和向量化状态。
create table if not exists note (
    id varchar(64) primary key,
    title varchar(500) not null,
    source_type varchar(20) not null,
    source_path varchar(1000) not null,
    content_hash varchar(128) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    word_count integer not null default 0,
    tags varchar(500),
    vectorized boolean not null default false,
    constraint uk_note_source_path unique (source_path)
    );

comment on table note is '笔记主表，保存笔记元数据与向量化状态';
comment on column note.id is '笔记 UUID';
comment on column note.title is '笔记标题';
comment on column note.source_type is '来源类型：obsidian/markdown/pdf/web';
comment on column note.source_path is '源文件路径或 URL';
comment on column note.content_hash is '内容哈希，用于判断文件是否变化';
comment on column note.created_at is '笔记创建时间';
comment on column note.updated_at is '最后更新时间';
comment on column note.word_count is '笔记字符数';
comment on column note.tags is '标签，逗号分隔';
comment on column note.vectorized is '是否已完成向量化';



-- 笔记关联表，保存双向链接推荐结果。
create table if not exists note_link (
                                         id bigserial primary key,
                                         source_note_id varchar(64) not null,
    target_note_id varchar(64) not null,
    confidence double precision not null default 0.0,
    reason varchar(1000),
    created_at timestamp not null default now(),
    constraint uk_note_link unique (source_note_id, target_note_id)
    );

comment on table note_link is '笔记关联表，保存双向链接推荐结果';
comment on column note_link.id is '关联记录主键';
comment on column note_link.source_note_id is '源笔记 ID';
comment on column note_link.target_note_id is '目标笔记 ID';
comment on column note_link.confidence is '关联置信度';
comment on column note_link.reason is '推荐理由';
comment on column note_link.created_at is '创建时间';



-- 周报记录表。
create table if not exists weekly_digest (
                                             id bigserial primary key,
                                             week_start date not null,
                                             week_end date not null,
                                             content text,
                                             sent_status varchar(30) not null default 'pending',
    sent_at timestamp,
    created_at timestamp not null default now()
    );

comment on table weekly_digest is '周报记录表，保存定期知识摘要';
comment on column weekly_digest.id is '周报主键';
comment on column weekly_digest.week_start is '周开始日期';
comment on column weekly_digest.week_end is '周结束日期';
comment on column weekly_digest.content is '摘要内容';
comment on column weekly_digest.sent_status is '发送状态';
comment on column weekly_digest.sent_at is '发送时间';
comment on column weekly_digest.created_at is '创建时间';

-- 防止同一周重复生成多份周报。
create unique index if not exists uk_weekly_digest_range
    on weekly_digest (week_start, week_end);

-- Spring AI PgVector 默认向量表。
create table if not exists vector_store (
    id uuid default uuid_generate_v4() primary key,
    content text,
    metadata json,
    embedding vector(1536)
    );

comment on table vector_store is 'Spring AI PgVector 向量表，保存知识块和 Embedding';
comment on column vector_store.id is '向量块主键';
comment on column vector_store.content is '知识块文本内容';
comment on column vector_store.metadata is '知识块元数据，例如 note_id、title、chunk_index';
comment on column vector_store.embedding is 'Embedding 向量，维度必须和模型输出一致';


