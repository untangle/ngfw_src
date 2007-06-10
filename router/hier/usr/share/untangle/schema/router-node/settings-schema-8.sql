-- settings schema for release-5.0

-------------
-- settings |
-------------

-- com.untangle.tran.nat.NatSettings.dhcpLeaseList
CREATE TABLE settings.n_router_dhcp_leases (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.tran.nat.NatSettings
CREATE TABLE settings.n_router_settings (
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
CREATE TABLE settings.n_router_redirects (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.tran.nat.NatSettings.dnsStaticHostList
CREATE TABLE settings.n_router_dns_hosts (
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
