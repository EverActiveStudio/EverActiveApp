CREATE SEQUENCE IF NOT EXISTS events_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE events
(
    id        BIGINT                      NOT NULL,
    user_id   BIGINT                      NOT NULL,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    data      JSON                        NOT NULL,
    CONSTRAINT pk_events PRIMARY KEY (id)
);

ALTER TABLE events
    ADD CONSTRAINT FK_EVENTS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);
