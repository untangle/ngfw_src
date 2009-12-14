-- events conversion for release-5.0
-- $HeadURL: svn://chef/work/src/uvm/hier/usr/share/untangle/schema/uvm/events-convert-13.sql $
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

ALTER SCHEMA events OWNER TO postgres;

CREATE TABLE events.n_login_evt (
    event_id    INT8 NOT NULL,
    login_name  TEXT,
    domain	TEXT,
    type	CHAR, -- LOGIN|UPDATE|LOGOUT
    time_stamp  TIMESTAMP,
    client_addr inet,
    PRIMARY KEY (event_id));
