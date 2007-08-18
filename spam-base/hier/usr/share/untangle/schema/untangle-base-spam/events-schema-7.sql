-- events schema for release-5.0.3
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

-----------
-- events |
-----------

CREATE TABLE events.n_spam_evt_smtp (
    event_id int8 NOT NULL,
    msg_id int8,
    score float4,
    is_spam bool,
    action char(1),
    vendor_name varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE events.n_spam_evt (
    event_id int8 NOT NULL,
    msg_id int8,
    score float4,
    is_spam bool,
    action char(1),
    vendor_name varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE events.n_spam_smtp_rbl_evt (
    event_id int8 NOT NULL,
    hostname varchar(255) NOT NULL,
    ipaddr inet NOT NULL,
    skipped bool NOT NULL,
    pl_endp_id int8 NOT NULL,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

----------------
-- constraints |
----------------

-- indices for reporting

CREATE INDEX n_spam_evt_smtp_ts_idx
    ON events.n_spam_evt_smtp (time_stamp);
CREATE INDEX n_spam_evt_ts_idx
    ON events.n_spam_evt (time_stamp);
CREATE INDEX n_spam_evt_mid_idx
    ON events.n_spam_evt (msg_id);
CREATE INDEX n_spam_evt_smtp_mid_idx
    ON events.n_spam_evt_smtp (msg_id);
CREATE INDEX n_spam_smtp_rbl_evt_ts_idx
    ON events.n_spam_smtp_rbl_evt (time_stamp);
