-- settings conversion for 3.3

-------------
-- settings |
-------------

ALTER TABLE settings.tr_reporting_settings
   ADD COLUMN email_detail bool;
UPDATE settings.tr_reporting_settings SET email_detail = false;

ALTER TABLE settings.tr_reporting_settings
   ADD COLUMN schedule int8;
-- when converting, defaults for schedule are generated in code

-- com.untangle.tran.reporting.Schedule
-- day of week based on java.util.Calendar constants
-- -1 (None), 1 (Sun), 2 (Mon), 3 (Tue), 4 (Wed), 5 (Thu), 6 (Fri), 7 (Sat)
CREATE TABLE settings.tr_reporting_sched (
    id int8 NOT NULL,
    daily bool NOT NULL,
    monthly_n_daily bool NOT NULL,
    monthly_n_day_of_wk int4 NOT NULL,
    monthly_n_first bool NOT NULL,
    PRIMARY KEY (id));

-- com.untangle.tran.reporting.WeeklyScheduleRule
-- day of week based on java.util.Calendar constants
-- 1 (Sun), 2 (Mon), 3 (Tue), 4 (Wed), 5 (Thu), 6 (Fri), 7 (Sat)
CREATE TABLE settings.tr_reporting_wk_sched_rule (
    id int8 NOT NULL,
    day int4 NOT NULL,
    PRIMARY KEY (id));

-- com.untangle.tran.reporting.ReportingSettings.weeklySchedule (list construct)
CREATE TABLE settings.tr_reporting_wk_sched (
    rule_id int8 NOT NULL,
    setting_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.tr_reporting_settings
    ADD CONSTRAINT fk_tr_reporting_settings_to_schedule
    FOREIGN KEY (schedule) REFERENCES settings.tr_reporting_sched;
