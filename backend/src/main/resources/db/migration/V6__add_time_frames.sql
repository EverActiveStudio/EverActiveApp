CREATE SEQUENCE IF NOT EXISTS time_frames_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE time_frames
(
    id             BIGINT  NOT NULL,
    group_id       BIGINT  NOT NULL,
    week_day_start INTEGER NOT NULL,
    hour_start     INTEGER NOT NULL,
    week_day_end   INTEGER NOT NULL,
    hour_end       INTEGER NOT NULL,
    CONSTRAINT pk_time_frames PRIMARY KEY (id)
);

ALTER TABLE time_frames
    ADD CONSTRAINT FK_TIME_FRAMES_ON_GROUP FOREIGN KEY (group_id) REFERENCES groups (id);
