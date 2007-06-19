-- settings convert for release 3.1
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

---------------------
-- no more varchars |
---------------------

ALTER TABLE settings.firewall_rule ADD COLUMN tmp text;
UPDATE settings.firewall_rule SET tmp = protocol_matcher;
ALTER TABLE settings.firewall_rule DROP COLUMN protocol_matcher;
ALTER TABLE settings.firewall_rule RENAME COLUMN tmp TO protocol_matcher;

ALTER TABLE settings.firewall_rule ADD COLUMN tmp text;
UPDATE settings.firewall_rule SET tmp = src_ip_matcher;
ALTER TABLE settings.firewall_rule DROP COLUMN src_ip_matcher;
ALTER TABLE settings.firewall_rule RENAME COLUMN tmp TO src_ip_matcher;

ALTER TABLE settings.firewall_rule ADD COLUMN tmp text;
UPDATE settings.firewall_rule SET tmp = dst_ip_matcher;
ALTER TABLE settings.firewall_rule DROP COLUMN dst_ip_matcher;
ALTER TABLE settings.firewall_rule RENAME COLUMN tmp TO dst_ip_matcher;

ALTER TABLE settings.firewall_rule ADD COLUMN tmp text;
UPDATE settings.firewall_rule SET tmp = src_port_matcher;
ALTER TABLE settings.firewall_rule DROP COLUMN src_port_matcher;
ALTER TABLE settings.firewall_rule RENAME COLUMN tmp TO src_port_matcher;

ALTER TABLE settings.firewall_rule ADD COLUMN tmp text;
UPDATE settings.firewall_rule SET tmp = dst_port_matcher;
ALTER TABLE settings.firewall_rule DROP COLUMN dst_port_matcher;
ALTER TABLE settings.firewall_rule RENAME COLUMN tmp TO dst_port_matcher;

ALTER TABLE settings.firewall_rule ADD COLUMN tmp text;
UPDATE settings.firewall_rule SET tmp = name;
ALTER TABLE settings.firewall_rule DROP COLUMN name;
ALTER TABLE settings.firewall_rule RENAME COLUMN tmp TO name;

ALTER TABLE settings.firewall_rule ADD COLUMN tmp text;
UPDATE settings.firewall_rule SET tmp = category;
ALTER TABLE settings.firewall_rule DROP COLUMN category;
ALTER TABLE settings.firewall_rule RENAME COLUMN tmp TO category;

ALTER TABLE settings.firewall_rule ADD COLUMN tmp text;
UPDATE settings.firewall_rule SET tmp = description;
ALTER TABLE settings.firewall_rule DROP COLUMN description;
ALTER TABLE settings.firewall_rule RENAME COLUMN tmp TO description;

------------------------
-- clean convert cruft |
------------------------

ALTER TABLE tr_firewall_settings DROP CONSTRAINT tr_firewall_settings_uk;
ALTER TABLE tr_firewall_settings ADD CONSTRAINT tr_firewall_settings_tid_key UNIQUE (tid);
