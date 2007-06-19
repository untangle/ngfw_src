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

CREATE TABLE tr_reporting_settings (id int8 NOT NULL, tid int8 NOT NULL UNIQUE, network_directory int8 NOT NULL, PRIMARY KEY (id));

ALTER TABLE tr_reporting_settings ADD CONSTRAINT FK85B769562C0555C FOREIGN KEY (network_directory) REFERENCES ipmaddr_dir;

ALTER TABLE tr_reporting_settings ADD CONSTRAINT FK85B76951446F FOREIGN KEY (tid) REFERENCES tid;
