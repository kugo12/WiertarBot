FROM flyway/flyway:latest-alpine

COPY migrations/ /flyway/migrations/
COPY flyway.toml /flyway/conf/

CMD ["migrate"]
