create table permission
(
    id        serial
        primary key,
    command   varchar(255) not null,
    whitelist text         not null,
    blacklist text         not null
);

create unique index permission_command
    on permission (command);

create table fbmessage
(
    id         serial
        primary key,
    message_id varchar(255) not null,
    thread_id  varchar(255) not null,
    author_id  varchar(255) not null,
    time       bigint       not null,
    message    text         not null,
    deleted_at bigint
);

create unique index fbmessage_message_id
    on fbmessage (message_id);

create table messagecountmilestone
(
    id        serial
        primary key,
    thread_id varchar(255) not null,
    count     integer      not null
);

create unique index messagecountmilestone_thread_id
    on messagecountmilestone (thread_id);
