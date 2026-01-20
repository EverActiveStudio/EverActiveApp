CREATE SEQUENCE IF NOT EXISTS rule_events_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE rule_events
(
    id        BIGINT                      NOT NULL,
    rule_id   BIGINT                      NOT NULL,
    user_id   BIGINT                      NOT NULL,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    is_failed BOOLEAN                     NOT NULL,
    CONSTRAINT pk_rule_events PRIMARY KEY (id)
);

ALTER TABLE rule_events
    ADD CONSTRAINT FK_RULE_EVENTS_ON_RULE FOREIGN KEY (rule_id) REFERENCES rules (id);

ALTER TABLE rule_events
    ADD CONSTRAINT FK_RULE_EVENTS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);
