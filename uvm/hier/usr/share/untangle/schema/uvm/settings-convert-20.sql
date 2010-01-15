-- Copyright (c) 2003-2008 Untangle, Inc.
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

-- com.untangle.uvm.RadiusServerSettings -- 7.2
CREATE TABLE settings.u_radius_server_settings (
    settings_id       INT8 NOT NULL,
    server	      TEXT NOT NULL,
    port	      INT4 NOT NULL,
    shared_secret     TEXT NOT NULL,
    PRIMARY KEY      (settings_id));

-- com.untangle.uvm.RadiusSettings -- 7.2
CREATE TABLE settings.u_radius_settings (
    id                        INT8 NOT NULL,
    radius_server_settings_id INT8 NOT NULL,
    PRIMARY KEY (id));
