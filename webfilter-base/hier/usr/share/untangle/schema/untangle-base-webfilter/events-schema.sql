-- events schema for release-5.0
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

-- com.untangle.tran.httpblocker.HttpBlockerEvent
CREATE TABLE events.n_webfilter_evt_blk (
    event_id int8 NOT NULL,
    request_id int8,
    action char(1),
    reason char(1),
    category varchar(255),
    vendor_name text,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- indices for reporting

CREATE INDEX n_webfilter_evt_blk_ts_idx ON events.n_webfilter_evt_blk (time_stamp);

-- bypass events & index
CREATE TABLE events.n_webfilter_evt_unblock (
    event_id int8 NOT NULL,
    policy_id int8 NOT NULL,
    time_stamp timestamp,
    vendor_name text,
    client_address inet,
    uid text,
    is_permanent bool,
    request_uri text,
    PRIMARY KEY (event_id));

CREATE INDEX n_webfilter_evt_unblock_ts_idx ON events.n_webfilter_evt_unblock (time_stamp);
