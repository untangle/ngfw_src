-- settings schema for release-5.0
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

-- com.untangle.tran.reporting.ReportingSettings
CREATE TABLE settings.n_reporting_settings (
    id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    email_detail bool,
    network_directory int8 NOT NULL,
    schedule int8,
    days_to_keep int4 NOT NULL,
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

-- foreign key constraints

ALTER TABLE settings.n_reporting_settings
    ADD CONSTRAINT fk_tr_reporting_settings
    FOREIGN KEY (tid) REFERENCES settings.u_tid;

ALTER TABLE settings.n_reporting_settings
    ADD CONSTRAINT fk_tr_reporting_settings_to_ipmaddr_dir
    FOREIGN KEY (network_directory) REFERENCES settings.u_ipmaddr_dir;

ALTER TABLE settings.n_reporting_settings
    ADD CONSTRAINT fk_tr_reporting_settings_to_schedule
    FOREIGN KEY (schedule) REFERENCES settings.n_reporting_sched;
