-- convert script for release 3.2
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

-- force spyware lists to reinitialize
UPDATE tr_spyware_settings set cookie_version = -1;
UPDATE tr_spyware_settings set activex_version = -1;
UPDATE tr_spyware_settings set subnet_version = -1;

-- indices

CREATE INDEX idx_spyware_rule_ar ON settings.tr_spyware_ar (rule_id);

CREATE INDEX idx_spyware_rule_cr ON settings.tr_spyware_cr (rule_id);

CREATE INDEX idx_spyware_rule_sr ON settings.tr_spyware_sr (rule_id);

