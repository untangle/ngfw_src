-- events conversion for release-4.1

ALTER TABLE events.pl_stats ADD COLUMN uid text;

CREATE SCHEMA reports;

CREATE TABLE reports.report_data_days (
        day_name text NOT NULL,
        day_begin date NOT NULL);


-- com.untangle.mvvm.user.LookupLogEvent
CREATE TABLE events.mvvm_lookup_evt (
    event_id    INT8 NOT NULL,
    lookup_key  INT8 NOT NULL,
    address     INET,
    username    TEXT,
    hostname    TEXT,
    lookup_time TIMESTAMP,
    time_stamp  TIMESTAMP,
    PRIMARY KEY (event_id));
