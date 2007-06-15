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

-- added column for the setup state
ALTER TABLE settings.tr_nat_settings ADD COLUMN setup_state INT2;

-- Indicate that the current settings are deprecated
UPDATE settings.tr_nat_settings SET setup_state=1;

-- Drop all of the constraints on NAT, these settings aren't needed after an upgrade.
ALTER TABLE settings.tr_dhcp_leases   DROP CONSTRAINT fk_tr_dhcp_leases;
ALTER TABLE settings.tr_dhcp_leases   DROP CONSTRAINT fk_tr_dhcp_leases_rule;

ALTER TABLE settings.tr_nat_dns_hosts DROP CONSTRAINT fk_tr_nat_dns_hosts;
ALTER TABLE settings.tr_nat_dns_hosts DROP CONSTRAINT fk_tr_nat_dns_hosts_rule;

ALTER TABLE settings.tr_nat_redirects DROP CONSTRAINT fk_tr_nat_redirects;
ALTER TABLE settings.tr_nat_redirects DROP CONSTRAINT fk_tr_nat_redirects_rule;

