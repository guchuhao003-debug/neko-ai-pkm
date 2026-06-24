alter table weekly_digest
    add column if not exists generated_at timestamp;

alter table weekly_digest
    add column if not exists note_count integer default 0;

alter table weekly_digest
    add column if not exists prompt_version varchar(50) default 'weekly_digest_v1';

alter table weekly_digest
    add column if not exists error_message varchar(1000);

alter table weekly_digest
    add column if not exists retry_count integer default 0;


