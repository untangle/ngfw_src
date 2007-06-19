-- settings schema for release 3.2
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

-------------
-- settings |
-------------

-- com.untangle.tran.nat.NatSettings.dhcpLeaseList
CREATE TABLE settings.tr_dhcp_leases (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.tran.nat.NatSettings
CREATE TABLE settings.tr_nat_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    setup_state INT2,
    nat_enabled bool,
    nat_internal_addr inet,
    nat_internal_subnet inet,
    dmz_enabled bool,
    dmz_address inet,
    dhcp_enabled bool,
    dhcp_s_address inet,
    dhcp_e_address inet,
    dhcp_lease_time int4,
    dns_enabled bool,
    dns_local_domain varchar(255),
    dmz_logging_enabled bool,
    PRIMARY KEY (settings_id));

-- com.untangle.tran.nat.NatSettings.redirectList
CREATE TABLE settings.tr_nat_redirects (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.tran.nat.NatSettings.dnsStaticHostList
CREATE TABLE settings.tr_nat_dns_hosts (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.tr_nat_settings
    ADD CONSTRAINT fk_tr_nat_settings
        FOREIGN KEY (tid) REFERENCES settings.tid;
