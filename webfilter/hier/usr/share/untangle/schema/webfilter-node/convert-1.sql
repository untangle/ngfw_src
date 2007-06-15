-- convert script for release 1.4
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

-- We forgot a semi-colon in schema-0

ALTER TABLE tr_httpblk_extensions ADD CONSTRAINT FKBC81FBBB871AAD3E FOREIGN KEY (rule_id) REFERENCES string_rule;

ALTER TABLE tr_httpblk_extensions ADD CONSTRAINT FKBC81FBBB1CAE658A FOREIGN KEY (setting_id) REFERENCES tr_httpblk_settings;
