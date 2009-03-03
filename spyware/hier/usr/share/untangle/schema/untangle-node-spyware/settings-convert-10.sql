-- settings conversion for release-5.0
-- $HeadURL: svn://chef/branch/prod/web-ui/work/src/spyware/hier/usr/share/untangle/schema/untangle-node-spyware/settings-convert-8.sql $
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

ALTER TABLE settings.n_spyware_cr DROP COLUMN position;
ALTER TABLE settings.n_spyware_ar DROP COLUMN position;
ALTER TABLE settings.n_spyware_sr DROP COLUMN position;
ALTER TABLE settings.n_spyware_wl DROP COLUMN position;


