-- settings conversion for release-5.0
-- $HeadURL: svn://chef/work/src/uvm/hier/usr/share/untangle/schema/uvm/settings-convert-12.sql $
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

UPDATE settings.u_node_persistent_state SET name = 'untangle-node-kav' WHERE name = 'untangle-node-hauri';
UPDATE settings.u_mackage_state SET mackage_name = 'untangle-node-kav' WHERE mackage_name = 'untangle-node-hauri';
