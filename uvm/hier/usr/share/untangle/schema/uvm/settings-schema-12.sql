-- settings schema for release-5.0

CREATE SCHEMA settings;

SET search_path TO settings,events,public;

CREATE SEQUENCE settings.hibernate_sequence;

-- com.untangle.uvm.security.AdminSettings
CREATE TABLE settings.u_admin_settings (
    admin_settings_id int8 NOT NULL,
    summary_period_id int8,
    PRIMARY KEY (admin_settings_id));

-- com.untangle.uvm.security.User
CREATE TABLE settings.u_user (
    id int8 NOT NULL,
    login text NOT NULL,
    password bytea NOT NULL,
    email text,
    name text NOT NULL,
    read_only bool NOT NULL,
    notes text,
    send_alerts bool,
    admin_setting_id int8,
    PRIMARY KEY (id));

-- com.untangle.uvm.UpgradeSettings
CREATE TABLE settings.u_upgrade_settings (
    upgrade_settings_id int8 NOT NULL,
    auto_upgrade bool NOT NULL,
    period int8 NOT NULL,
    PRIMARY KEY (upgrade_settings_id));

-- com.untangle.uvm.BrandingSettings
CREATE TABLE settings.uvm_branding_settings (
    settings_id int8 NOT NULL,
    company_name text,
    company_url text,
    logo bytea,
    contact_name text,
    contact_email text,
    PRIMARY KEY (settings_id));

-- com.untangle.uvm.MailSettings
CREATE TABLE settings.u_mail_settings (
    mail_settings_id int8 NOT NULL,
    report_email text,
    smtp_host text,
    from_address text,
    smtp_port int4 NOT NULL,
    use_tls bool NOT NULL,
    auth_user text,
    auth_pass text,
    local_host_name text,
    use_mx_records bool NOT NULL,
    PRIMARY KEY (mail_settings_id));

-- com.untangle.uvm.logging.LoggingSettings
CREATE TABLE settings.u_logging_settings (
    settings_id int8 NOT NULL,
    syslog_enabled bool NOT NULL,
    syslog_host text,
    syslog_port int4,
    syslog_facility int4,
    syslog_threshold int4,
    PRIMARY KEY (settings_id));

-- com.untangle.uvm.policy.Policy
CREATE TABLE settings.u_policy (
    id int8 NOT NULL,
    is_default bool NOT NULL,
    name text NOT NULL,
    notes text,
    PRIMARY KEY (id));

-- com.untangle.uvm.policy.UserPolicyRuleSet
CREATE TABLE settings.u_user_policy_rules (
    set_id int8 NOT NULL,
    PRIMARY KEY (set_id));

-- com.untangle.uvm.policy.UserPolicyRule
CREATE TABLE settings.u_user_policy_rule (
    rule_id int8 NOT NULL,
    protocol_matcher text,
    client_ip_matcher text,
    server_ip_matcher text,
    client_port_matcher text,
    server_port_matcher text,
    client_intf_matcher text,
    server_intf_matcher text,
    policy_id int8,
    is_inbound bool NOT NULL,
    name text,
    category text,
    description text,
    live bool,
    alert bool,
    log bool,
    set_id int8,
    position int4,
    start_time time,
    end_time time,
    day_of_week_matcher text,
    user_matcher text,
    invert_entire_duration bool NOT NULL,
    PRIMARY KEY (rule_id));

-- com.untangle.uvm.policy.SystemPolicyRule
CREATE TABLE settings.u_system_policy_rule (
    rule_id int8 NOT NULL,
    client_intf int2 NOT NULL,
    server_intf int2 NOT NULL,
    policy_id int8,
    is_inbound bool NOT NULL,
    name text,
    category text,
    description text,
    live bool,
    alert bool,
    log bool,
    PRIMARY KEY (rule_id));

-- com.untangle.uvm.engine.NodePersistentState.args
CREATE TABLE settings.u_node_args (
    tps_id int8 NOT NULL,
    arg text NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (tps_id, position));

-- com.untangle.uvm.engine.MackageState
CREATE TABLE settings.u_mackage_state (
    id int8 NOT NULL,
    mackage_name text NOT NULL,
    extra_name text,
    enabled bool NOT NULL,
    PRIMARY KEY (id));

-- com.untangle.uvm.engine.NodeManagerState
CREATE TABLE settings.u_node_manager_state (
    id int8 NOT NULL,
    last_tid int8,
    PRIMARY KEY (id));

-- com.untangle.uvm.Period
CREATE TABLE settings.u_period (
    period_id int8 NOT NULL,
    hour int4 NOT NULL,
    minute int4 NOT NULL,
    sunday bool,
    monday bool,
    tuesday bool,
    wednesday bool,
    thursday bool,
    friday bool,
    saturday bool,
    PRIMARY KEY (period_id));

-- com.untangle.uvm.tran.NodePreferences
CREATE TABLE settings.u_node_preferences (
    id int8 NOT NULL,
    tid int8,
    red int4,
    green int4,
    blue int4,
    alpha int4,
    PRIMARY KEY (id));

-- com.untangle.uvm.tran.StringRule
CREATE TABLE settings.u_string_rule (
    rule_id int8 NOT NULL,
    string text,
    name text,
    category text,
    description text,
    live bool,
    alert bool,
    log bool,
    PRIMARY KEY (rule_id));

-- com.untangle.uvm.security.Tid
CREATE TABLE settings.u_tid (
    id int8 NOT NULL,
    policy_id int8,
    PRIMARY KEY (id));

-- com.untangle.uvm.engine.NodePersistentState
CREATE TABLE settings.u_node_persistent_state (
    id int8 NOT NULL,
    name text NOT NULL,
    tid int8,
    public_key bytea NOT NULL,
    target_state text NOT NULL,
    PRIMARY KEY (id));

-- com.untangle.uvm.tran.IPMaddrDirectory
CREATE TABLE settings.u_ipmaddr_dir (
    id int8 NOT NULL,
    notes text,
    PRIMARY KEY (id));

-- com.untangle.uvm.tran.MimeTypeRule
CREATE TABLE settings.u_mimetype_rule (
    rule_id int8 NOT NULL,
    mime_type text,
    name text,
    category text,
    description text,
    live bool,
    alert bool,
    log bool,
    PRIMARY KEY (rule_id));

-- com.untangle.uvm.tran.IPMaddrDirectory.entries
CREATE TABLE settings.u_ipmaddr_dir_entries (
    ipmaddr_dir_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (ipmaddr_dir_id, position));

-- com.untangle.uvm.tran.IPMaddrRule
CREATE TABLE settings.u_ipmaddr_rule (
    rule_id int8 NOT NULL,
    ipmaddr inet,
    name text,
    category text,
    description text,
    live bool,
    alert bool,
    log bool,
    PRIMARY KEY (rule_id));


-- com.untangle.uvm.snmp.SnmpSettings
CREATE TABLE settings.u_snmp_settings (
    snmp_settings_id int8 NOT NULL,
    enabled bool,
    port int4,
    com_str text,
    sys_contact text,
    sys_location text,
    send_traps bool,
    trap_host text,
    trap_com text,
    trap_port int4,
    PRIMARY KEY (snmp_settings_id));


-- com.untangle.uvm.addrbook.RepositorySettings
CREATE TABLE settings.u_ab_repository_settings (
    settings_id int8 NOT NULL,
    superuser text,
    superuser_pass text,
    domain text,
    ldap_host text,
    ou_filter text,
    port int4,
    PRIMARY KEY (settings_id));


-- com.untangle.uvm.addrbook.AddressBookSettings
CREATE TABLE settings.u_ab_settings (
    settings_id int8 NOT NULL,
    ad_repo_settings int8 NOT NULL,
    ab_configuration char(1) NOT NULL,
    PRIMARY KEY (settings_id));

-- com.untangle.uvm.networking.DynamicDNSSettings -- 3.2
CREATE TABLE settings.u_ddns_settings (
    settings_id int8 NOT NULL,
    enabled     BOOL,
    provider    TEXT,
    login       TEXT,
    password    TEXT,
    PRIMARY KEY (settings_id));

-- com.untangle.uvm.networking.DhcpLeaseRule -- 3.2
CREATE TABLE settings.u_dhcp_lease_rule (
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

-- com.untangle.uvm.networking.DnsStaticHostRule -- 3.2
CREATE TABLE settings.u_dns_static_host_rule (
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


-- com.untangle.uvm.networking.Interface -- 3.2
CREATE TABLE settings.u_network_intf (
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

-- com.untangle.uvm.networking.IPNetworkRule -- 3.2
CREATE TABLE settings.u_ip_network (
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

-- com.untangle.uvm.networking.Route -- 3.2
CREATE TABLE settings.u_network_route (
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

-- com.untangle.uvm.networking.NetworkSpace -- 3.2
CREATE TABLE settings.u_network_space (
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

-- com.untangle.uvm.networking.RedirectRule -- 3.2
CREATE TABLE settings.u_redirect_rule (
    rule_id           INT8 NOT NULL,
    is_dst_redirect   BOOL,
    redirect_port     INT4,
    redirect_addr     INET,
    src_intf_matcher  TEXT,
    dst_intf_matcher  TEXT,
    protocol_matcher  TEXT,
    src_ip_matcher    TEXT,
    dst_ip_matcher    TEXT,
    src_port_matcher  TEXT,
    dst_port_matcher  TEXT,
    name              TEXT,
    category          TEXT,
    description       TEXT,
    live              BOOL,
    alert             BOOL,
    log               BOOL,
    is_local_redirect BOOL,
    primary key      (rule_id));

-- Table linking network settings to redirects -- 3.2x
-- com.untangle.uvm.networking.NetworkSpacesSettings.redirectList -- 3.2
CREATE TABLE settings.u_redirects (
    setting_id  INT8 NOT NULL,
    rule_id     INT8 NOT NULL,
    position    INT4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.uvm.networking.NetworkSpacesSettings -- 3.2
CREATE TABLE settings.u_network_settings (
    settings_id INT8 NOT NULL,
    is_enabled BOOL,
    setup_state INT4,
    default_route INET,
    dns_1 INET,
    dns_2 INET,
    completed_setup BOOL,
    PRIMARY KEY (settings_id));

-- com.untangle.uvm.networking.AccessSettings -- 4.2
CREATE TABLE settings.u_access_settings (
    settings_id          INT8 NOT NULL,
    allow_ssh            BOOL,
    allow_insecure       BOOL,
    allow_outside        BOOL,
    restrict_outside     BOOL,
    outside_network      INET,
    outside_netmask      INET,
    allow_outside_admin  BOOL,
    allow_outside_quaran BOOL,
    allow_outside_report BOOL,
    PRIMARY KEY          (settings_id));

-- com.untangle.uvm.networking.MiscSettings -- 4.2
CREATE TABLE settings.u_misc_settings (
    settings_id          INT8 NOT NULL,
    report_exceptions    BOOL,
    tcp_window_scaling   BOOL,
    post_configuration   TEXT,
    custom_rules         TEXT,
    PRIMARY KEY          (settings_id));

-- com.untangle.uvm.networking.AddressSettings -- 4.2
CREATE TABLE settings.u_address_settings (
    settings_id          INT8 NOT NULL,
    https_port           INT4,
    hostname             TEXT,
    is_hostname_public   BOOL,
    has_public_address   BOOL,
    public_ip_addr       INET,
    public_port          INT4,
    PRIMARY KEY          (settings_id));

-- Services settings
-- com.untangle.uvm.networking.ServicesSettingsImpl -- 3.2
CREATE TABLE settings.u_network_services (
       settings_id        INT8 NOT NULL,
       is_dhcp_enabled    BOOL,
       dhcp_start_address INET,
       dhcp_end_address   INET,
       dhcp_lease_time    INT4,
       dns_enabled        BOOL,
       dns_local_domain   TEXT,
       primary key        (settings_id));

-- com.untangle.uvm.networking.ServicesSettingsImpl.dhcpLeaseList -- 3.2
CREATE TABLE settings.u_dhcp_lease_list (
       setting_id   INT8 NOT NULL,
       rule_id      INT8 NOT NULL,
       position     INT4 NOT NULL,
       PRIMARY KEY  (setting_id, position));

-- com.untangle.uvm.networking.ServicesSettingsImpl.dnsStaticHostList -- 3.2
CREATE TABLE settings.u_dns_host_list (
       setting_id   INT8 NOT NULL,
       rule_id      INT8 NOT NULL,
       position     INT4 NOT NULL,
       PRIMARY KEY  (setting_id, position));

-- com.untangle.uvm.portal.Bookmark -- 4.0
CREATE TABLE settings.n_portal_bookmark (
        id               INT8 NOT NULL,
        name             TEXT,
        target           TEXT,
        application_name TEXT,
        PRIMARY KEY      (id));

-- com.untangle.uvm.portal.PortalUser -- 4.0
CREATE TABLE settings.n_portal_user (
        id               INT8 NOT NULL,
        uid              TEXT,
        live             BOOL,
        description      TEXT,
        group_id         INT8,
        home_settings_id INT8,
        settings_id      INT8,
        position         INT4,
        PRIMARY KEY      (id));

-- com.untangle.uvm.portal.PortalUser.bookmarks -- 4.0
CREATE TABLE settings.n_portal_user_bm_mt (
    settings_id int8 NOT NULL,
    bookmark_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

-- com.untangle.uvm.portal.PortalGroup -- 4.0
CREATE TABLE settings.n_portal_group (
        id               INT8 NOT NULL,
        name             TEXT,
        description      TEXT,
        home_settings_id INT8,
        settings_id      INT8,
        position         INT4,
        PRIMARY KEY      (id));

-- com.untangle.uvm.portal.PortalGroup.bookmarks -- 4.0
CREATE TABLE settings.n_portal_group_bm_mt (
    settings_id int8 NOT NULL,
    bookmark_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

-- com.untangle.uvm.portal.PortalGlobal -- 4.0
CREATE TABLE settings.n_portal_global (
        id               INT8 NOT NULL,
        auto_create_users BOOL,
        login_page_title TEXT,
        login_page_text  TEXT,
        home_settings_id INT8,
        PRIMARY KEY      (id));

-- com.untangle.uvm.portal.PortalGlobal.bookmarks -- 4.0
CREATE TABLE settings.n_portal_global_bm_mt (
    settings_id int8 NOT NULL,
    bookmark_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

-- com.untangle.uvm.security.PortalHomeSettings
CREATE TABLE settings.n_portal_home_settings (
    id              INT8 NOT NULL,
    home_page_title TEXT,
    home_page_text  TEXT,
    bookmark_table_title TEXT,
    show_exploder   BOOL,
    show_bookmarks  BOOL,
    show_add_bookmark BOOL,
    idle_timeout    INT8,
    PRIMARY KEY (id));

-- com.untangle.uvm.security.PortalSettings
CREATE TABLE settings.n_portal_settings (
    id int8 NOT NULL,
    global_settings_id INT8,
    PRIMARY KEY (id));

-- com.untangle.uvm.networking.PPPoEConnectionRule -- 4.1
CREATE TABLE settings.u_pppoe_connection (
    rule_id           INT8 NOT NULL,
    name              TEXT,
    category          TEXT,
    description       TEXT,
    live              BOOL,
    alert             BOOL,
    log               BOOL,
    settings_id       INT8,
    position          INT4,
    username          TEXT,
    password          TEXT,
    intf              INT2,
    keepalive         BOOL,
    secret_field      TEXT,
    PRIMARY KEY       (rule_id));

-- com.untangle.uvm.networking.PPPoESettings -- 4.1
CREATE TABLE settings.u_pppoe (
    settings_id       INT8 NOT NULL,
    live              BOOL,
    PRIMARY KEY      (settings_id));

-- com.untangle.uvm.user.WMISettings -- 4.1
CREATE TABLE settings.u_wmi_settings (
    settings_id       INT8 NOT NULL,
    live              BOOL,
    scheme            TEXT,
    address           INET,
    port              INT4,
    username          TEXT,
    password          TEXT,
    PRIMARY KEY       (settings_id));

----------------
-- constraints |
----------------

-- list indeces

CREATE INDEX u_idx_string_rule ON settings.u_string_rule (string);

-- foreign key constraints

ALTER TABLE settings.u_tid
    ADD CONSTRAINT fk_tid_policy
    FOREIGN KEY (policy_id) REFERENCES settings.u_policy;

ALTER TABLE settings.user_policy_rule
    ADD CONSTRAINT fk_user_policy_rule_parent
    FOREIGN KEY (set_id) REFERENCES settings.u_user_policy_rules;

ALTER TABLE settings.user_policy_rule
    ADD CONSTRAINT fk_user_policy_rule_policy
    FOREIGN KEY (policy_id) REFERENCES settings.u_policy;

ALTER TABLE settings.u_system_policy_rule
    ADD CONSTRAINT fk_system_policy_rule_policy
    FOREIGN KEY (policy_id) REFERENCES settings.u_policy;

ALTER TABLE settings.u_admin_settings
    ADD CONSTRAINT fk_admin_settings
    FOREIGN KEY (summary_period_id) REFERENCES settings.u_period;

ALTER TABLE settings.u_user
    ADD CONSTRAINT fk_uvm_user
    FOREIGN KEY (admin_setting_id) REFERENCES settings.u_admin_settings;

ALTER TABLE settings.u_upgrade_settings
    ADD CONSTRAINT fk_upgrade_settings
    FOREIGN KEY (period) REFERENCES settings.u_period;

ALTER TABLE settings.node_args
    ADD CONSTRAINT fk_node_args
    FOREIGN KEY (tps_id) REFERENCES settings.u_node_persistent_state;

ALTER TABLE settings.node_preferences
    ADD CONSTRAINT fk_node_preferences
    FOREIGN KEY (tid) REFERENCES settings.u_tid;

ALTER TABLE settings.node_persistent_state
    ADD CONSTRAINT fk_node_persistent_state
    FOREIGN KEY (tid) REFERENCES settings.u_tid;

ALTER TABLE settings.u_ipmaddr_dir_entries
    ADD CONSTRAINT fk_ipmaddr_dir_entries
    FOREIGN KEY (ipmaddr_dir_id) REFERENCES settings.u_ipmaddr_dir;

-- Network spaces

-- ALTER TABLE u_ip_network
--       ADD CONSTRAINT fk_uvm_ip_network_space
--       FOREIGN KEY (space_id) REFERENCES u_network_space;

-- ALTER TABLE u_network_intf
--       ADD CONSTRAINT fk_uvm_intf_network_settings
--       FOREIGN KEY (settings_id) REFERENCES u_network_settings;

-- ALTER TABLE u_network_intf
--       ADD CONSTRAINT fk_uvm_intf_network_space
--       FOREIGN KEY (network_space) REFERENCES u_network_space;

-- ALTER TABLE u_network_route
--       ADD CONSTRAINT fk_uvm_route_network_settings
--       FOREIGN KEY (settings_id) REFERENCES network_settings;

-- ALTER TABLE u_network_route
--       ADD CONSTRAINT fk_uvm_route_network_space
--       FOREIGN KEY (network_space) REFERENCES u_network_space;

-- ALTER TABLE u_network_space
--       ADD CONSTRAINT fk_uvm_space_network_settings
--       FOREIGN KEY (settings_id) REFERENCES network_settings;

-- ALTER TABLE u_redirects
--       ADD CONSTRAINT fk_uvm_redirect_network_settings
--       FOREIGN KEY (setting_id) REFERENCES network_settings;

-- ALTER TABLE u_redirects
--       ADD CONSTRAINT fk_uvm_redirect_redirect
--       FOREIGN KEY (rule_id) REFERENCES redirect_rule;

-- -- Services settings
ALTER TABLE u_dhcp_lease_list
      ADD CONSTRAINT fk_uvm_lease_services
      FOREIGN KEY (setting_id) REFERENCES u_network_services;

ALTER TABLE u_dhcp_lease_list
      ADD CONSTRAINT fk_uvm_lease_lease
      FOREIGN KEY (rule_id) REFERENCES u_dhcp_lease_rule;

ALTER TABLE u_dns_host_list
      ADD CONSTRAINT fk_uvm_dns_services
      FOREIGN KEY (setting_id) REFERENCES u_network_services;

ALTER TABLE u_dns_host_list
      ADD CONSTRAINT fk_uvm_dns_dns
      FOREIGN KEY (rule_id) REFERENCES u_dns_static_host_rule;

-- Portals
ALTER TABLE settings.n_portal_group
    ADD CONSTRAINT fk_portal_group_parent
    FOREIGN KEY (settings_id) REFERENCES settings.n_portal_settings;

ALTER TABLE settings.n_portal_user
    ADD CONSTRAINT fk_portal_user_parent
    FOREIGN KEY (settings_id) REFERENCES settings.n_portal_settings;
