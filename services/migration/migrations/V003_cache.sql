create unlogged table cache_entries (
    name varchar(255) not null,
    key varchar(255) not null,
    value bytea,
    expires_at timestamp with time zone,
    primary key (name, key)
);
