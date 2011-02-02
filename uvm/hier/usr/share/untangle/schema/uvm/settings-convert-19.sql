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

ALTER TABLE settings.u_policy ADD COLUMN parent_id int8;

ALTER TABLE settings.u_access_settings ADD COLUMN block_page_port INT4;

UPDATE settings.u_access_settings SET block_page_port = 80;
ALTER TABLE settings.u_access_settings ALTER COLUMN block_page_port SET NOT NULL;

-- Create a column for whether or not a user has write access
ALTER TABLE settings.u_user ADD COLUMN write_access BOOL;
ALTER TABLE settings.u_user ADD COLUMN reports_access BOOL;

UPDATE settings.u_user SET write_access = 't', reports_access = 't';

ALTER TABLE settings.u_user ALTER COLUMN write_access SET NOT NULL;
ALTER TABLE settings.u_user ALTER COLUMN reports_access SET NOT NULL;

ALTER TABLE settings.u_user DROP COLUMN read_only;

