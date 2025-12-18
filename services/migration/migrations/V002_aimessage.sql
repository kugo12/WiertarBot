create table ai_message
(
    id              bigserial
        primary key,
    conversation_id varchar(255) not null,
    message_type    varchar(50)  not null,
    content         text         not null,
    created_at      timestamp    not null default CURRENT_TIMESTAMP,
    message_id      varchar(255) not null
);

create index ai_message_conversation_id
    on ai_message (conversation_id);

create index idx_ai_message_conversation_id_id
    on ai_message (conversation_id, id);
