-- settings conversion for 3.3
-- $HeadURL$
-- Copyright (c) 2003-2007 Untangle, Inc. 
--
-- This program is free software; you can redistribute it and/or modify
-- it under the terms of the GNU General Public License, version 2,
-- as published by the Free Software Foundation.
--
-- This program is distributed in the hope that it will be useful, but
-- AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
-- NONINFRINGEMENT.  See the GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with this program; if not, write to the Free Software
-- Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
--


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
