-- events conversion for release-4.2

CREATE TABLE events.event_data_days (
        day_name text NOT NULL,
        day_begin date NOT NULL);

DROP INDEX events.pl_endp_cdate_idx;

ALTER TABLE events.pl_endp DROP COLUMN create_date;

CREATE INDEX pl_endp_ts_idx ON events.pl_endp (time_stamp);
