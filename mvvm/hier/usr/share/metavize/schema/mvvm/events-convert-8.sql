-- events conversion for release-4.1

ALTER TABLE events.pl_stats ADD COLUMN uid text;

CREATE SCHEMA reports;

CREATE TABLE reports.report_data_days (
        day_name text NOT NULL,
        day_begin date NOT NULL);
