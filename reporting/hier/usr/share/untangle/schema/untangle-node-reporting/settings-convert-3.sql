-- settings conversion for release-3.2
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

-- drop incorrect constraint from 3.1 (which used network_directory to reference ipmaddr_dir table)
ALTER TABLE settings.tr_reporting_settings
    DROP CONSTRAINT fk_tr_reporting_settings;

-- recreate (e.g., correctly create) constraint (which uses tid to reference tid table)
ALTER TABLE settings.tr_reporting_settings
    ADD CONSTRAINT fk_tr_reporting_settings
    FOREIGN KEY (tid) REFERENCES settings.tid;

-- create constraint (which uses network_directory to reference ipmaddr_dir table)
ALTER TABLE settings.tr_reporting_settings
    ADD CONSTRAINT fk_tr_reporting_settings_to_ipmaddr_dir
    FOREIGN KEY (network_directory) REFERENCES settings.ipmaddr_dir;
