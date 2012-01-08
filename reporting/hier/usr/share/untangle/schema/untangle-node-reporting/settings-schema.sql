-------------
-- settings |
-------------

-- com.untangle.tran.reporting.ReportingSettings
CREATE TABLE settings.n_reporting_settings (
    id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    email_detail bool NOT NULL,
    attachment_size_limit int8 NOT NULL,
    network_directory int8 NOT NULL,
    schedule int8,
    nightly_hour int4 NOT NULL,
    nightly_minute int4 NOT NULL,
    db_retention int4 NOT NULL,
    file_retention int4 NOT NULL,
    reporting_users TEXT,
    PRIMARY KEY (id));

-- com.untangle.tran.reporting.Schedule
-- day of week based on java.util.Calendar constants
-- 1 (Sun), 2 (Mon), 3 (Tue), 4 (Wed), 5 (Thu), 6 (Fri), 7 (Sat)
CREATE TABLE settings.n_reporting_sched (
    id int8 NOT NULL,
    daily bool,
    monthly_n_daily bool,
    monthly_n_day_of_wk int4 NOT NULL,
    monthly_n_first bool,
    PRIMARY KEY (id));

-- com.untangle.tran.reporting.WeeklyScheduleRule
-- day of week based on java.util.Calendar constants
-- 1 (Sun), 2 (Mon), 3 (Tue), 4 (Wed), 5 (Thu), 6 (Fri), 7 (Sat)
CREATE TABLE settings.n_reporting_wk_sched_rule (
    id int8 NOT NULL,
    day int4 NOT NULL,
    PRIMARY KEY (id));

-- com.untangle.tran.reporting.ReportingSettings.weeklySchedule (list construct)
CREATE TABLE settings.n_reporting_wk_sched (
    rule_id int8 NOT NULL,
    setting_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

----------------
-- constraints |
----------------

ALTER TABLE settings.n_reporting_settings
    ADD CONSTRAINT fk_tr_reporting_settings
    FOREIGN KEY (tid) REFERENCES settings.u_tid;

ALTER TABLE settings.n_reporting_settings
    ADD CONSTRAINT fk_tr_reporting_settings_to_ipmaddr_dir
    FOREIGN KEY (network_directory) REFERENCES settings.u_ipmaddr_dir;

ALTER TABLE settings.n_reporting_settings
    ADD CONSTRAINT fk_tr_reporting_settings_to_schedule
    FOREIGN KEY (schedule) REFERENCES settings.n_reporting_sched;
