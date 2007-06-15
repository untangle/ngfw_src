-- settings schema for release 3.1
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

-- com.untangle.tran.firewall.FirewallRule
CREATE TABLE settings.firewall_rule (
    rule_id int8 NOT NULL,
    is_traffic_blocker bool,
    protocol_matcher text,
    src_ip_matcher text,
    dst_ip_matcher text,
    src_port_matcher text,
    dst_port_matcher text,
    inbound bool,
    outbound bool,
    name text,
    category text,
    description text,
    live bool,
    alert bool,
    log bool,
    PRIMARY KEY (rule_id));

-- com.untangle.tran.firewall.FirewallSettings.firewallRuleList
CREATE TABLE settings.tr_firewall_rules (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.tran.firewall.FirewallSettings
CREATE TABLE settings.tr_firewall_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    is_quickexit BOOL,
    is_reject_silent BOOL,
    is_default_accept BOOL,
    PRIMARY KEY (settings_id));

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.tr_firewall_rules
    ADD CONSTRAINT fk_tr_firewall_rules
        FOREIGN KEY (rule_id) REFERENCES settings.firewall_rule;
ALTER TABLE settings.tr_firewall_rules
    ADD CONSTRAINT fk_tr_firewall_rules_settings
        FOREIGN KEY (setting_id) REFERENCES settings.tr_firewall_settings;
ALTER TABLE settings.tr_firewall_settings
    ADD CONSTRAINT fk_tr_firewall_settings
        FOREIGN KEY (tid) REFERENCES settings.tid;
