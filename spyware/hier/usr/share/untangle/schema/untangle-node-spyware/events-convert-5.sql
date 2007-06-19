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

-- fix events.tr_spyware_evt_access

DROP TABLE events.tr_spyware_new;

CREATE TABLE events.tr_spyware_new AS
    SELECT evt.event_id, pl_endp.event_id AS pl_endp_id, ipmaddr,
           ident::text, blocked, evt.time_stamp
    FROM events.tr_spyware_evt_access evt JOIN pl_endp USING (session_id);

DROP TABLE events.tr_spyware_evt_access;
ALTER TABLE events.tr_spyware_new RENAME TO tr_spyware_evt_access;
ALTER TABLE events.tr_spyware_evt_access ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_spyware_evt_access ADD PRIMARY KEY (event_id);

-- fix events.tr_spyware_evt_activex

DROP TABLE events.tr_spyware_new;

CREATE TABLE events.tr_spyware_new AS
    SELECT evt.event_id, request_id,
           ident::text, evt.time_stamp
    FROM events.tr_spyware_evt_activex evt;

DROP TABLE events.tr_spyware_evt_activex;
ALTER TABLE events.tr_spyware_new RENAME TO tr_spyware_evt_activex;
ALTER TABLE events.tr_spyware_evt_activex ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_spyware_evt_activex ADD PRIMARY KEY (event_id);

-- fix events.tr_spyware_evt_cookie

DROP TABLE events.tr_spyware_new;

CREATE TABLE events.tr_spyware_new AS
    SELECT evt.event_id, request_id,
           ident::text, to_server, evt.time_stamp
    FROM events.tr_spyware_evt_cookie evt;

DROP TABLE events.tr_spyware_evt_cookie;
ALTER TABLE events.tr_spyware_new RENAME TO tr_spyware_evt_cookie;
ALTER TABLE events.tr_spyware_evt_cookie ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_spyware_evt_cookie ADD PRIMARY KEY (event_id);

-- fix events.tr_spyware_evt_blacklist

DROP TABLE events.tr_spyware_new;

CREATE TABLE events.tr_spyware_new AS
    SELECT evt.event_id, request_id,
           evt.time_stamp
    FROM events.tr_spyware_evt_blacklist evt;

DROP TABLE events.tr_spyware_evt_blacklist;
ALTER TABLE events.tr_spyware_new RENAME TO tr_spyware_evt_blacklist;
ALTER TABLE events.tr_spyware_evt_blacklist ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_spyware_evt_blacklist ADD PRIMARY KEY (event_id);

-- recreate indexes

CREATE INDEX tr_spyware_cookie_rid_idx
    ON events.tr_spyware_evt_cookie (request_id);
CREATE INDEX tr_spyware_bl_rid_idx
    ON events.tr_spyware_evt_blacklist (request_id);
CREATE INDEX tr_spyware_ax_rid_idx
    ON events.tr_spyware_evt_activex (request_id);
CREATE INDEX tr_spyware_acc_sid_idx
    ON events.tr_spyware_evt_access (pl_endp_id);

-- indices for reporting

DROP INDEX tr_spyware_acc_sid_idx;
CREATE INDEX tr_spyware_acc_plepid_idx
    ON events.tr_spyware_evt_access (pl_endp_id);
