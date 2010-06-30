-- events conversion for release-4.1
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
