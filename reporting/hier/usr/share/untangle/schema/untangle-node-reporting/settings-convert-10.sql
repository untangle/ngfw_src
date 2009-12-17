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

ALTER TABLE settings.n_reporting_settings
   ADD COLUMN attachment_size_limit int8;
UPDATE settings.n_reporting_settings SET attachment_size_limit = 10;
ALTER TABLE settings.n_reporting_settings 
   ALTER COLUMN attachment_size_limit SET NOT NULL;

UPDATE settings.n_reporting_settings
   SET db_retention = 7
   WHERE db_retention = 14;
