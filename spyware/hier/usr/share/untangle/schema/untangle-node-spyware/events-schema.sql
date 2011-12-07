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

-----------
-- events |
-----------

-- com.untangle.tran.spyware.SpywareAccessEvent
CREATE TABLE events.n_spyware_evt_access (
    event_id int8 NOT NULL,
    pl_endp_id int8,
    ipmaddr inet,
    ident text,
    blocked bool,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.spyware.SpywareActiveXEvent
CREATE TABLE events.n_spyware_evt_activex (
    event_id int8 NOT NULL,
    request_id int8,
    ident text,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.spyware.SpywareCookieEvent
CREATE TABLE events.n_spyware_evt_cookie (
    event_id int8 NOT NULL,
    request_id int8,
    ident text,
    to_server bool,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.spyware.SpywareBlacklistEvent
CREATE TABLE events.n_spyware_evt_blacklist (
    event_id int8 NOT NULL,
    request_id int8,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- indeces for reporting

CREATE INDEX n_spyware_cookie_rid_idx ON events.n_spyware_evt_cookie (request_id);
CREATE INDEX n_spyware_bl_rid_idx ON events.n_spyware_evt_blacklist (request_id);
CREATE INDEX n_spyware_ax_rid_idx ON events.n_spyware_evt_activex (request_id);
CREATE INDEX n_spyware_acc_plepid_idx ON events.n_spyware_evt_access (pl_endp_id);
CREATE INDEX n_spyware_evt_cookie_ts_idx ON events.n_spyware_evt_cookie (time_stamp);
CREATE INDEX n_spyware_evt_blacklist_ts_idx ON events.n_spyware_evt_blacklist (time_stamp);
CREATE INDEX n_spyware_evt_activex_ts_idx ON events.n_spyware_evt_activex (time_stamp);
CREATE INDEX n_spyware_evt_access_ts_idx ON events.n_spyware_evt_access (time_stamp);
