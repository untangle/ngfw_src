-- settings conversion for release-3.2

-- Added for AddressBook
-- com.metavize.mvvm.addrbook.RepositorySettings
CREATE TABLE settings.ab_repository_settings (
    settings_id int8 NOT NULL,
    superuser_dn text,
    superuser_pass text,
    search_base text,
    ldap_host text,
    port int4,
    PRIMARY KEY (settings_id));


-- Added for AddressBook
-- com.metavize.mvvm.addrbook.AddressBookSettings

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
-- com.metavize.mvvm.networking.DynamicDNSSettings
CREATE TABLE settings.mvvm_ddns_settings (
    settings_id int8 NOT NULL,
    enabled     BOOL,
    provider    TEXT,
    login       TEXT,
    password    TEXT,
    PRIMARY KEY (settings_id));

-- com.metavize.mvvm.networking.DhcpLeaseRule -- 3.2
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

-- com.metavize.mvvm.networking.DnsStaticHostRule -- 3.2
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


-- com.metavize.mvvm.networking.Interface -- 3.2
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

-- com.metavize.mvvm.networking.IPNetworkRule -- 3.2
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

-- com.metavize.mvvm.networking.Route -- 3.2
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

-- com.metavize.mvvm.networking.NetworkSpace -- 3.2
CREATE TABLE settings.mvvm_network_space (
    rule_id              INT8 NOT NULL,
    papers               INT8,
    is_traffic_forwarded BOOL,
    is_dhcp_enabled      BOOL,
    is_nat_enabled       BOOL,
    nat_address          INET,
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

-- com.metavize.mvvm.networking.RedirectRule -- 3.2
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

-- com.metavize.mvvm.networking.NetworkSpacesSettings -- 3.2
CREATE TABLE settings.mvvm_network_settings (
    settings_id INT8 NOT NULL,
    is_enabled BOOL,
    setup_state INT4,
    default_route INET,
    dns_1 INET,
    dns_2 INET,
    PRIMARY KEY (settings_id));

-- com.metavize.mvvm.networking.ServicesSettingsImpl -- 3.2
CREATE TABLE mvvm_network_services (
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
-- ALTER TABLE mvvm_dhcp_leases 
--       ADD CONSTRAINT fk_mvvm_lease_services
--       FOREIGN KEY (setting_id) REFERENCES mvvm_network_services;

-- ALTER TABLE mvvm_dhcp_leases
--       ADD CONSTRAINT fk_mvvm_lease_lease
--       FOREIGN KEY (rule_id) REFERENCES mvvm_dhcp_lease_rule;

-- ALTER TABLE mvvm_dns_hosts 
--       ADD CONSTRAINT fk_mvvm_dns_services
--       FOREIGN KEY (setting_id) REFERENCES mvvm_network_services;

-- ALTER TABLE mvvm_dns_hosts
--       ADD CONSTRAINT fk_mvvm_dns_dns
--       FOREIGN KEY (rule_id) REFERENCES mvvm_dns_static_host_rule;

