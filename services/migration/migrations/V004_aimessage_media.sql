alter table ai_message add column media bytea compression lz4;
alter table ai_message alter column content set compression lz4;
