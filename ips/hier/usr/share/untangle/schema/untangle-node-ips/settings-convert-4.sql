-- settings conversion for release-5.0
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

ALTER TABLE settings.TR_IDS_SETTINGS RENAME TO n_ips_settings;
ALTER TABLE settings.TR_IDS_VARIABLE RENAME TO n_ips_variable;
ALTER TABLE settings.TR_IDS_IMMUTABLE_VARIABLES RENAME TO n_ips_immutable_variables;
ALTER TABLE settings.TR_IDS_MUTABLE_VARIABLES RENAME TO n_ips_mutable_variables;
ALTER TABLE settings.TR_IDS_RULE RENAME TO n_ips_rule;
