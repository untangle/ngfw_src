-- settings conversion for release-3.2
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

-- Added for AddressBook
-- com.untangle.mvvm.addrbook.RepositorySettings
CREATE TABLE settings.ab_repository_settings (
    settings_id int8 NOT NULL,
    superuser_dn text,
    superuser_pass text,
    search_base text,
    ldap_host text,
    port int4,
    PRIMARY KEY (settings_id));


-- Added for AddressBook
-- com.untangle.mvvm.addrbook.AddressBookSettings

CREATE TABLE settings.ab_settings (
    settings_id int8 NOT NULL,
    ad_repo_settings int8 NOT NULL,
    ab_configuration char(1) NOT NULL,
    PRIMARY KEY (settings_id));

UPDATE tid SET policy_id = NULL WHERE id IN
    (SELECT tid.id FROM tid
     LEFT JOIN transform_persistent_state ON tid.id = tid
     WHERE target_state IS NULL AND NOT policy_id IS NULL);

DELETE FROM user_policy_rule WHERE set_id IS NULL;

-- Add read_only column to mvvm_user
ALTER TABLE settings.mvvm_user ADD COLUMN read_only bool;
UPDATE settings.mvvm_user SET read_only = false;
ALTER TABLE settings.mvvm_user ALTER COLUMN read_only SET NOT NULL;

-- Network spaces

-- Add the table for the dynamic DNS settings
-- com.untangle.mvvm.networking.DynamicDNSSettings
CREATE TABLE settings.mvvm_ddns_settings (
    settings_id int8 NOT NULL,
    enabled     BOOL,
    provider    TEXT,
    login       TEXT,
    password    TEXT,
    PRIMARY KEY (settings_id));

-- com.untangle.mvvm.networking.DhcpLeaseRule -- 3.2
-- moved from dhcp_lease_rule to mvvm_dhcp_lease_rule
-- ALTER TABLE settings.dhcp_lease_rule RENAME TO mvvm_dhcp_lease_rule;

CREATE TABLE settings.mvvm_dhcp_lease_rule AS
    SELECT rule_id, mac_address::text, hostname::text, static_address, is_resolve_mac,
           name::text, category::text, description::text, live, alert, log
           FROM settings.dhcp_lease_rule;

-- Just in case NAT exists, delete the constraints.
ALTER TABLE settings.tr_dhcp_leases   DROP CONSTRAINT fk_tr_dhcp_leases;
ALTER TABLE settings.tr_dhcp_leases   DROP CONSTRAINT fk_tr_dhcp_leases_rule;

DROP TABLE settings.dhcp_lease_rule;
ALTER TABLE settings.mvvm_dhcp_lease_rule ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.mvvm_dhcp_lease_rule ADD PRIMARY KEY (rule_id);


-- This is just in case NAT was never installed.
-- com.untangle.mvvm.networking.DhcpLeaseRule -- 3.2
CREATE TABLE settings.mvvm_dhcp_lease_rule (
    rule_id        INT8 NOT NULL,
    mac_address    TEXT,
    hostname       TEXT,
    static_address INET,
    is_resolve_mac BOOL,
    name           TEXT,
    category       TEXT,
    description    TEXT,
    live           BOOL,
    alert          BOOL,
    log            BOOL,
    PRIMARY KEY    (rule_id));

-- com.untangle.mvvm.networking.DnsStaticHostRule -- 3.2
-- moved from dns_static_host_rule to mvvm_dns_static_host_rule
CREATE TABLE settings.mvvm_dns_static_host_rule AS
    SELECT rule_id, hostname_list::text, static_address, 
           name::text, category::text, description::text, live, alert, log
           FROM settings.dns_static_host_rule;

-- Just in case NAT exists, delete the constraints.
ALTER TABLE settings.tr_nat_dns_hosts DROP CONSTRAINT fk_tr_nat_dns_hosts;
ALTER TABLE settings.tr_nat_dns_hosts DROP CONSTRAINT fk_tr_nat_dns_hosts_rule;

DROP TABLE settings.dns_static_host_rule;
ALTER TABLE settings.mvvm_dns_static_host_rule ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.mvvm_dns_static_host_rule ADD PRIMARY KEY (rule_id);

-- This is just in case NAT was never installed.
-- com.untangle.mvvm.networking.DnsStaticHostRule -- 3.2
CREATE TABLE settings.mvvm_dns_static_host_rule (
    rule_id        INT8 NOT NULL,
    hostname_list  TEXT,
    static_address INET,
    name           TEXT,
    category       TEXT,
    description    TEXT,
    live           BOOL,
    alert          BOOL,
    log            BOOL,
    PRIMARY KEY    (rule_id));

-- com.untangle.mvvm.networking.ServicesSettingsImpl.dhcpLeaseList -- 3.2
CREATE TABLE settings.mvvm_dhcp_lease_list (
       setting_id   INT8 NOT NULL,
       rule_id      INT8 NOT NULL,
       position     INT4 NOT NULL,
       PRIMARY KEY  (setting_id, position));

-- com.untangle.mvvm.networking.ServicesSettingsImpl.dnsStaticHostList -- 3.2
CREATE TABLE settings.mvvm_dns_host_list (
       setting_id   INT8 NOT NULL,
       rule_id      INT8 NOT NULL,
       position     INT4 NOT NULL,
       PRIMARY KEY  (setting_id, position));

-- com.untangle.mvvm.networking.Interface -- 3.2
CREATE TABLE settings.mvvm_network_intf (
    rule_id        INT8 NOT NULL,
    argon_intf     INT2,
    network_space  INT8,
    media          INT4,
    pingable       BOOL,
    name           TEXT,
    category       TEXT,
    description    TEXT,
    live           BOOL,
    alert          BOOL,
    log            BOOL,
    settings_id    INT8,
    position       INT4,
    PRIMARY KEY    (rule_id));

-- com.untangle.mvvm.networking.IPNetworkRule -- 3.2
CREATE TABLE settings.mvvm_ip_network (
    rule_id     INT8 NOT NULL,
    network     TEXT,
    name        TEXT, 
    category    TEXT,
    description TEXT,
    live        BOOL,
    alert       BOOL,
    log         BOOL,
    space_id    INT8,
    position    INT4,
    PRIMARY KEY (rule_id));

-- com.untangle.mvvm.networking.Route -- 3.2
CREATE TABLE settings.mvvm_network_route (
    rule_id       INT8 NOT NULL,
    network_space INT8,
    destination   TEXT,
    next_hop      INET,
    name          TEXT,
    category      TEXT,
    description   TEXT,
    live          BOOL,
    alert         BOOL,
    log           BOOL,
    settings_id   INT8,
    position      INT4,
    PRIMARY KEY   (rule_id));

-- com.untangle.mvvm.networking.NetworkSpace -- 3.2
CREATE TABLE settings.mvvm_network_space (
    rule_id              INT8 NOT NULL,
    papers               INT8,
    is_traffic_forwarded BOOL,
    is_dhcp_enabled      BOOL,
    is_nat_enabled       BOOL,
    nat_address          INET,
    nat_space            INT8,
    dmz_host_enabled     BOOL,
    dmz_host             INET,
    dmz_host_logging     BOOL,
    mtu                  INT4,
    name                 TEXT,           
    category             TEXT,
    description          TEXT,
    live                 BOOL,
    alert                BOOL,
    log                  BOOL,
    settings_id          INT8,
    position             INT4,
    PRIMARY KEY          (rule_id));

-- com.untangle.mvvm.networking.RedirectRule -- 3.2
-- (moved to the mvvm from nat)
CREATE TABLE settings.mvvm_redirect_rule AS
    SELECT rule_id, is_dst_redirect, redirect_port, redirect_addr,
           src_intf_matcher::text, dst_intf_matcher::text, protocol_matcher::text,
           src_ip_matcher::text,   dst_ip_matcher::text,
           src_port_matcher::text, dst_port_matcher::text, 
           name::text, category::text, description::text, live, alert, log
           FROM settings.redirect_rule;

-- Just in case NAT exists, delete the constraints.
ALTER TABLE settings.tr_nat_redirects DROP CONSTRAINT fk_tr_nat_redirects;
ALTER TABLE settings.tr_nat_redirects DROP CONSTRAINT fk_tr_nat_redirects_rule;

DROP TABLE settings.redirect_rule;
ALTER TABLE settings.mvvm_redirect_rule ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.mvvm_redirect_rule ADD PRIMARY KEY (rule_id);

-- This is just in case NAT was never installed.
-- com.untangle.mvvm.networking.RedirectRule -- 3.2
CREATE TABLE settings.mvvm_redirect_rule (
    rule_id          INT8 NOT NULL,
    is_dst_redirect  BOOL,
    redirect_port    INT4,
    redirect_addr    INET,
    src_intf_matcher TEXT,
    dst_intf_matcher TEXT,
    protocol_matcher TEXT,
    src_ip_matcher   TEXT, 
    dst_ip_matcher   TEXT,
    src_port_matcher TEXT,
    dst_port_matcher TEXT,
    name             TEXT,
    category         TEXT,
    description      TEXT,
    live             BOOL,
    alert            BOOL,
    log              BOOL,
    primary key      (rule_id));



-- Table linking network settings to redirects -- 3.2x
CREATE TABLE settings.mvvm_redirects (
    setting_id  INT8 NOT NULL,
    rule_id     INT8 NOT NULL,
    position    INT4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.mvvm.networking.NetworkSpacesSettings -- 3.2
CREATE TABLE settings.mvvm_network_settings (
    settings_id INT8 NOT NULL,
    is_enabled BOOL,
    setup_state INT4,
    default_route INET,
    dns_1 INET,
    dns_2 INET,
    PRIMARY KEY (settings_id));

-- com.untangle.mvvm.networking.ServicesSettingsImpl -- 3.2
CREATE TABLE settings.mvvm_network_services (
       settings_id        INT8 NOT NULL,
       is_dhcp_enabled    BOOL,
       dhcp_start_address INET,
       dhcp_end_address   INET,
       dhcp_lease_time    INT4,
       dns_enabled        BOOL, 
       dns_local_domain   TEXT,
       primary key        (settings_id));

-- The number of inter-dependencies is a just too much for hibernate.
-- ALTER TABLE mvvm_ip_network
--       ADD CONSTRAINT fk_mvvm_ip_network_space
--       FOREIGN KEY (space_id) REFERENCES mvvm_network_space;

-- ALTER TABLE mvvm_network_intf
--       ADD CONSTRAINT fk_mvvm_intf_network_settings
--       FOREIGN KEY (settings_id) REFERENCES mvvm_network_settings;

-- ALTER TABLE mvvm_network_intf
--       ADD CONSTRAINT fk_mvvm_intf_network_space
--       FOREIGN KEY (network_space) REFERENCES mvvm_network_space;

-- ALTER TABLE mvvm_network_route
--       ADD CONSTRAINT fk_mvvm_route_network_settings
--       FOREIGN KEY (settings_id) REFERENCES network_settings;

-- ALTER TABLE mvvm_network_route
--       ADD CONSTRAINT fk_mvvm_route_network_space
--       FOREIGN KEY (network_space) REFERENCES mvvm_network_space;

-- ALTER TABLE mvvm_network_space
--       ADD CONSTRAINT fk_mvvm_space_network_settings
--       FOREIGN KEY (settings_id) REFERENCES network_settings;

-- ALTER TABLE mvvm_redirects
--       ADD CONSTRAINT fk_mvvm_redirect_network_settings
--       FOREIGN KEY (setting_id) REFERENCES network_settings;

-- ALTER TABLE mvvm_redirects
--       ADD CONSTRAINT fk_mvvm_redirect_redirect
--       FOREIGN KEY (rule_id) REFERENCES redirect_rule;

-- -- Services settings
ALTER TABLE settings.mvvm_dhcp_lease_list
      ADD CONSTRAINT fk_mvvm_lease_services
      FOREIGN KEY (setting_id) REFERENCES settings.mvvm_network_services;

ALTER TABLE settings.mvvm_dhcp_lease_list
      ADD CONSTRAINT fk_mvvm_lease_lease
      FOREIGN KEY (rule_id) REFERENCES settings.mvvm_dhcp_lease_rule;

ALTER TABLE settings.mvvm_dns_hosts 
      ADD CONSTRAINT fk_mvvm_dns_services
      FOREIGN KEY (setting_id) REFERENCES settings.mvvm_network_services;

ALTER TABLE settings.mvvm_dns_hosts
      ADD CONSTRAINT fk_mvvm_dns_dns
      FOREIGN KEY (rule_id) REFERENCES settings.mvvm_dns_static_host_rule;

