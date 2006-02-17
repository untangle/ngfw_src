-- settings schema for release-3.2

CREATE SCHEMA settings;

SET search_path TO settings,events,public;

CREATE SEQUENCE settings.hibernate_sequence;

-- com.metavize.mvvm.security.AdminSettings
CREATE TABLE settings.admin_settings (
    admin_settings_id int8 NOT NULL,
    summary_period_id int8,
    PRIMARY KEY (admin_settings_id));

-- com.metavize.mvvm.security.User
CREATE TABLE settings.mvvm_user (
    id int8 NOT NULL,
    login text NOT NULL,
    password bytea NOT NULL,
    name text NOT NULL,
    read_only bool NOT NULL,
    notes text,
    send_alerts bool,
    admin_setting_id int8,
    PRIMARY KEY (id));

-- com.metavize.mvvm.UpgradeSettings
CREATE TABLE settings.upgrade_settings (
    upgrade_settings_id int8 NOT NULL,
    auto_upgrade bool NOT NULL,
    period int8 NOT NULL,
    PRIMARY KEY (upgrade_settings_id));

-- com.metavize.mvvm.MailSettings
CREATE TABLE settings.mail_settings (
    mail_settings_id int8 NOT NULL,
    report_email text,
    smtp_host text,
    from_address text,
    smtp_port int4 NOT NULL,
    use_tls bool NOT NULL,
    auth_user text,
    auth_pass text,
    local_host_name text,
    PRIMARY KEY (mail_settings_id));

-- com.metavize.mvvm.logging.LoggingSettings
CREATE TABLE settings.logging_settings (
    settings_id int8 NOT NULL,
    syslog_enabled bool NOT NULL,
    syslog_host text,
    syslog_port int4,
    syslog_facility int4,
    syslog_threshold int4,
    PRIMARY KEY (settings_id));

-- com.metavize.mvvm.policy.Policy
CREATE TABLE settings.policy (
    id int8 NOT NULL,
    is_default bool NOT NULL,
    name text NOT NULL,
    notes text,
    PRIMARY KEY (id));

-- com.metavize.mvvm.policy.UserPolicyRuleSet
CREATE TABLE settings.mvvm_user_policy_rules (
    set_id int8 NOT NULL,
    PRIMARY KEY (set_id));

-- com.metavize.mvvm.policy.UserPolicyRule
CREATE TABLE settings.user_policy_rule (
    rule_id int8 NOT NULL,
    protocol_matcher text,
    client_ip_matcher text,
    server_ip_matcher text,
    client_port_matcher text,
    server_port_matcher text,
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
    set_id int8,
    position int4,
    PRIMARY KEY (rule_id));

-- com.metavize.mvvm.policy.SystemPolicyRule
CREATE TABLE settings.system_policy_rule (
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

-- com.metavize.mvvm.engine.TransformPersistentState.args
CREATE TABLE settings.transform_args (
    tps_id int8 NOT NULL,
    arg text NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (tps_id, position));

-- com.metavize.mvvm.engine.TransformManagerState
CREATE TABLE settings.transform_manager_state (
    id int8 NOT NULL,
    last_tid int8,
    PRIMARY KEY (id));

-- com.metavize.mvvm.Period
CREATE TABLE settings.period (
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

-- com.metavize.mvvm.tran.TransformPreferences
CREATE TABLE settings.transform_preferences (
    id int8 NOT NULL,
    tid int8,
    red int4,
    green int4,
    blue int4,
    alpha int4,
    PRIMARY KEY (id));

-- com.metavize.mvvm.tran.StringRule
CREATE TABLE settings.string_rule (
    rule_id int8 NOT NULL,
    string text,
    name text,
    category text,
    description text,
    live bool,
    alert bool,
    log bool,
    PRIMARY KEY (rule_id));

-- com.metavize.mvvm.security.Tid
CREATE TABLE settings.tid (
    id int8 NOT NULL,
    policy_id int8,
    PRIMARY KEY (id));

-- com.metavize.mvvm.engine.TransformPersistentState
CREATE TABLE settings.transform_persistent_state (
    id int8 NOT NULL,
    name text NOT NULL,
    tid int8,
    public_key bytea NOT NULL,
    target_state text NOT NULL,
    PRIMARY KEY (id));

-- com.metavize.mvvm.tran.IPMaddrDirectory
CREATE TABLE settings.ipmaddr_dir (
    id int8 NOT NULL,
    notes text,
    PRIMARY KEY (id));

-- com.metavize.mvvm.tran.MimeTypeRule
CREATE TABLE settings.mimetype_rule (
    rule_id int8 NOT NULL,
    mime_type text,
    name text,
    category text,
    description text,
    live bool,
    alert bool,
    log bool,
    PRIMARY KEY (rule_id));

-- com.metavize.mvvm.tran.IPMaddrDirectory.entries
CREATE TABLE settings.ipmaddr_dir_entries (
    ipmaddr_dir_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (ipmaddr_dir_id, position));

-- com.metavize.mvvm.tran.IPMaddrRule
CREATE TABLE settings.ipmaddr_rule (
    rule_id int8 NOT NULL,
    ipmaddr inet,
    name text,
    category text,
    description text,
    live bool,
    alert bool,
    log bool,
    PRIMARY KEY (rule_id));


-- com.metavize.mvvm.snmp.SnmpSettings
CREATE TABLE settings.snmp_settings (
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


-- com.metavize.mvvm.addrbook.RepositorySettings
CREATE TABLE settings.ab_repository_settings (
    settings_id int8 NOT NULL,
    superuser_dn text,
    superuser_pass text,
    search_base text,
    ldap_host text,
    port int4,
    PRIMARY KEY (settings_id));


-- com.metavize.mvvm.addrbook.AddressBookSettings
CREATE TABLE settings.ab_settings (
    settings_id int8 NOT NULL,
    ad_repo_settings int8 NOT NULL,
    ab_configuration char(1) NOT NULL,
    PRIMARY KEY (settings_id));

-- com.metavize.mvvm.networking.DynamicDNSSettings
CREATE TABLE mvvm_ddns_settings (
    settings_id int8 NOT NULL,
    enabled     BOOL,
    provider    TEXT,
    login       TEXT,
    password    TEXT,
    PRIMARY KEY (settings_id));

----------------
-- constraints |
----------------

-- list indeces

CREATE INDEX idx_string_rule ON settings.string_rule (string);

-- foreign key constraints

ALTER TABLE settings.tid
    ADD CONSTRAINT fk_tid_policy
    FOREIGN KEY (policy_id) REFERENCES settings.policy;

ALTER TABLE settings.user_policy_rule
    ADD CONSTRAINT fk_user_policy_rule_parent
    FOREIGN KEY (set_id) REFERENCES settings.mvvm_user_policy_rules;

ALTER TABLE settings.user_policy_rule
    ADD CONSTRAINT fk_user_policy_rule_policy
    FOREIGN KEY (policy_id) REFERENCES settings.policy;

ALTER TABLE settings.system_policy_rule
    ADD CONSTRAINT fk_system_policy_rule_policy
    FOREIGN KEY (policy_id) REFERENCES settings.policy;

ALTER TABLE settings.admin_settings
    ADD CONSTRAINT fk_admin_settings
    FOREIGN KEY (summary_period_id) REFERENCES settings.period;

ALTER TABLE settings.mvvm_user
    ADD CONSTRAINT fk_mvvm_user
    FOREIGN KEY (admin_setting_id) REFERENCES settings.admin_settings;

ALTER TABLE settings.upgrade_settings
    ADD CONSTRAINT fk_upgrade_settings
    FOREIGN KEY (period) REFERENCES settings.period;

ALTER TABLE settings.transform_args
    ADD CONSTRAINT fk_transform_args
    FOREIGN KEY (tps_id) REFERENCES settings.transform_persistent_state;

ALTER TABLE settings.transform_preferences
    ADD CONSTRAINT fk_transform_preferences
    FOREIGN KEY (tid) REFERENCES settings.tid;

ALTER TABLE settings.transform_persistent_state
    ADD CONSTRAINT fk_transform_persistent_state
    FOREIGN KEY (tid) REFERENCES settings.tid;

ALTER TABLE settings.ipmaddr_dir_entries
    ADD CONSTRAINT fk_ipmaddr_dir_entries
    FOREIGN KEY (ipmaddr_dir_id) REFERENCES settings.ipmaddr_dir;

