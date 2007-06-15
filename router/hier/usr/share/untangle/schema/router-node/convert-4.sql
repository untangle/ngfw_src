-- convert script for release 2.5
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

-----------------------------------
-- move old tables to new schemas |
-----------------------------------

-- com.untangle.tran.nat.DhcpLeaseRule
CREATE TABLE settings.dhcp_lease_rule AS SELECT * FROM public.dhcp_lease_rule;

ALTER TABLE settings.dhcp_lease_rule
    ADD CONSTRAINT dhcp_lease_rule_pkey PRIMARY KEY (rule_id);
ALTER TABLE settings.dhcp_lease_rule
    ALTER COLUMN rule_id SET NOT NULL;

-- com.untangle.tran.nat.NatSettings.dhcpLeaseList
CREATE TABLE settings.tr_dhcp_leases AS SELECT * FROM public.tr_dhcp_leases;

ALTER TABLE settings.tr_dhcp_leases
    ADD CONSTRAINT tr_dhcp_leases_pkey PRIMARY KEY (setting_id, position);
ALTER TABLE settings.tr_dhcp_leases
    ALTER COLUMN setting_id SET NOT NULL;
ALTER TABLE settings.tr_dhcp_leases
    ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.tr_dhcp_leases
    ALTER COLUMN position SET NOT NULL;

-- com.untangle.tran.nat.RedirectRule
CREATE TABLE settings.redirect_rule AS SELECT * FROM public.redirect_rule;

ALTER TABLE settings.redirect_rule
    ADD CONSTRAINT redirect_rule_pkey PRIMARY KEY (rule_id);
ALTER TABLE settings.redirect_rule
    ALTER COLUMN rule_id SET NOT NULL;

-- com.untangle.tran.nat.NatSettings (adding column dmz_logging_enabled)
CREATE TABLE settings.tr_nat_settings (
    settings_id,
    tid,
    nat_enabled,
    nat_internal_addr,
    nat_internal_subnet,
    dmz_enabled,
    dmz_address,
    dhcp_enabled,
    dhcp_s_address,
    dhcp_e_address,
    dhcp_lease_time,
    dns_enabled,
    dns_local_domain,
    dmz_logging_enabled)
AS SELECT settings_id, tid, nat_enabled, nat_internal_addr,
          nat_internal_subnet, dmz_enabled, dmz_address, dhcp_enabled,
          dhcp_s_address, dhcp_e_address, dhcp_lease_time, dns_enabled,
          dns_local_domain, false
   FROM public.tr_nat_settings;

ALTER TABLE settings.tr_nat_settings
    ADD CONSTRAINT tr_nat_settings_pkey PRIMARY KEY (settings_id);
ALTER TABLE settings.tr_nat_settings
    ADD CONSTRAINT tr_nat_settings_uk UNIQUE (tid);
ALTER TABLE settings.tr_nat_settings
    ALTER COLUMN settings_id SET NOT NULL;
ALTER TABLE settings.tr_nat_settings
    ALTER COLUMN tid SET NOT NULL;

-- com.untangle.tran.nat.NatSettings.redirectList
CREATE TABLE settings.tr_nat_redirects
    AS SELECT * FROM public.tr_nat_redirects;

ALTER TABLE settings.tr_nat_redirects
    ADD CONSTRAINT tr_nat_redirects_pkey PRIMARY KEY (setting_id, position);
ALTER TABLE settings.tr_nat_redirects
    ALTER COLUMN setting_id SET NOT NULL;
ALTER TABLE settings.tr_nat_redirects
    ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.tr_nat_redirects
    ALTER COLUMN position SET NOT NULL;

-- com.untangle.tran.nat.NatSettings.dnsStaticHostList
CREATE TABLE settings.tr_nat_dns_hosts
    AS SELECT * from public.tr_nat_dns_hosts;

ALTER TABLE settings.tr_nat_dns_hosts
    ADD CONSTRAINT tr_nat_dns_hosts_pkey PRIMARY KEY (setting_id, position);
ALTER TABLE settings.tr_nat_dns_hosts
    ALTER COLUMN setting_id SET NOT NULL;
ALTER TABLE settings.tr_nat_dns_hosts
    ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.tr_nat_dns_hosts
    ALTER COLUMN position SET NOT NULL;

-- com.untangle.tran.nat.DnsStaticHostRule
CREATE TABLE settings.dns_static_host_rule
    AS SELECT * FROM public.dns_static_host_rule;

ALTER TABLE settings.dns_static_host_rule
    ADD CONSTRAINT dns_static_host_rule_pkey PRIMARY KEY (rule_id);
ALTER TABLE settings.dns_static_host_rule
    ALTER COLUMN rule_id SET NOT NULL;

-- com.untangle.tran.nat.DhcpLeaseEvent
CREATE TABLE events.tr_nat_evt_dhcp AS SELECT * FROM public.tr_nat_evt_dhcp;

ALTER TABLE events.tr_nat_evt_dhcp
    ADD CONSTRAINT tr_nat_evt_dhcp_pkey PRIMARY KEY (event_id);
ALTER TABLE events.tr_nat_evt_dhcp
    ALTER COLUMN event_id SET NOT NULL;

-- com.untangle.tran.nat.DhcpAbsoluteLease
CREATE TABLE events.dhcp_abs_lease AS SELECT * FROM public.dhcp_abs_lease;

ALTER TABLE events.dhcp_abs_lease
    ADD CONSTRAINT dhcp_abs_lease_pkey PRIMARY KEY (event_id);
ALTER TABLE events.dhcp_abs_lease
    ALTER COLUMN event_id SET NOT NULL;

-- com.untangle.tran.nat.DhcpAbsoluteEvent
CREATE TABLE events.tr_nat_evt_dhcp_abs
    AS SELECT * FROM public.tr_nat_evt_dhcp_abs;

ALTER TABLE events.tr_nat_evt_dhcp_abs
    ADD CONSTRAINT tr_nat_evt_dhcp_abs_pkey PRIMARY KEY (event_id);
ALTER TABLE events.tr_nat_evt_dhcp_abs
    ALTER COLUMN event_id SET NOT NULL;

-- com.untangle.tran.nat.DhcpAbsoluteEvent.absoluteLeaseList
CREATE TABLE events.tr_nat_evt_dhcp_abs_leases
    AS SELECT * FROM public.tr_nat_evt_dhcp_abs_leases;

ALTER TABLE events.tr_nat_evt_dhcp_abs_leases
    ADD CONSTRAINT tr_nat_evt_dhcp_abs_leasespkey PRIMARY KEY (event_id, position);
ALTER TABLE events.tr_nat_evt_dhcp_abs
    ALTER COLUMN event_id SET NOT NULL;

-------------------------
-- recreate constraints |
-------------------------

-- foreign key constraints

ALTER TABLE settings.tr_nat_settings
    ADD CONSTRAINT fk_tr_nat_settings
        FOREIGN KEY (tid) REFERENCES settings.tid;
ALTER TABLE settings.tr_nat_redirects
    ADD CONSTRAINT fk_tr_nat_redirects
        FOREIGN KEY (setting_id) REFERENCES settings.tr_nat_settings;
ALTER TABLE settings.tr_nat_redirects
    ADD CONSTRAINT fk_tr_nat_redirects_rule
        FOREIGN KEY (rule_id) REFERENCES settings.redirect_rule;
ALTER TABLE settings.tr_dhcp_leases
    ADD CONSTRAINT fk_tr_dhcp_leases
        FOREIGN KEY (setting_id) REFERENCES settings.tr_nat_settings;
ALTER TABLE settings.tr_dhcp_leases
    ADD CONSTRAINT fk_tr_dhcp_leases_rule
        FOREIGN KEY (rule_id) REFERENCES settings.dhcp_lease_rule;
ALTER TABLE settings.tr_nat_dns_hosts
    ADD CONSTRAINT fk_tr_nat_dns_hosts
        FOREIGN KEY (setting_id) REFERENCES settings.tr_nat_settings;
ALTER TABLE settings.tr_nat_dns_hosts
    ADD CONSTRAINT fk_tr_nat_dns_hosts_rule
        FOREIGN KEY (rule_id) REFERENCES settings.dns_static_host_rule;

-------------------------
-- drop old constraints |
-------------------------

-- foreign key constraints

ALTER TABLE public.tr_nat_settings DROP CONSTRAINT fk2f819dc21446f;
ALTER TABLE public.tr_nat_redirects DROP CONSTRAINT fkcbbf56381cae658a;
ALTER TABLE public.tr_nat_redirects DROP CONSTRAINT fkcbbf5638871aad3e;
ALTER TABLE public.tr_dhcp_leases DROP CONSTRAINT fka6469261cae658a;
ALTER TABLE public.tr_dhcp_leases DROP CONSTRAINT fka646926871aad3e;
ALTER TABLE public.tr_nat_dns_hosts DROP CONSTRAINT fk956bcb361cae658a;
ALTER TABLE public.tr_nat_dns_hosts DROP CONSTRAINT fk956bcb36871aad3e;

--------------------
-- drop old tables |
--------------------

DROP TABLE public.dhcp_lease_rule;
DROP TABLE public.tr_dhcp_leases;
DROP TABLE public.redirect_rule;
DROP TABLE public.tr_nat_settings;
DROP TABLE public.tr_nat_redirects;
DROP TABLE public.tr_nat_dns_hosts;
DROP TABLE public.dns_static_host_rule;
DROP TABLE public.tr_nat_evt_dhcp;
DROP TABLE public.dhcp_abs_lease;
DROP TABLE public.tr_nat_evt_dhcp_abs;
DROP TABLE public.tr_nat_evt_dhcp_abs_leases;

---------------
-- new tables |
---------------

-- com.untangle.tran.nat.RedirectEvent
CREATE TABLE events.tr_nat_redirect_evt (
    event_id int8 NOT NULL,
    session_id int4,
    rule_id int8,
    rule_index int4,
    is_dmz bool,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.nat.NatStatisticEvent
CREATE TABLE events.tr_nat_statistic_evt (
    event_id int8 NOT NULL,
    nat_sessions int4,
    dmz_sessions int4,
    tcp_incoming int4,
    tcp_outgoing int4,
    udp_incoming int4,
    udp_outgoing int4,
    icmp_incoming int4,
    icmp_outgoing int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

------------
-- analyze |
------------

ANALYZE events.tr_nat_evt_dhcp;
ANALYZE events.dhcp_abs_lease;
ANALYZE events.tr_nat_evt_dhcp_abs;
ANALYZE events.tr_nat_evt_dhcp_abs_leases;
