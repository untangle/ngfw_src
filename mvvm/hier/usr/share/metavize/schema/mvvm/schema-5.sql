-- schema for release-3.1

CREATE SCHEMA settings;
CREATE SCHEMA events;
SET search_path TO settings,events,public;

-------------
-- settings |
-------------

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
CREATE TABLE LOGGING_SETTINGS (
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

-----------
-- events |
-----------

-- com.metavize.mvvm.engine.LoginEvent
CREATE TABLE events.mvvm_login_evt (
    event_id int8 NOT NULL,
    client_addr inet,
    login text,
    local bool,
    succeeded bool,
    reason char(1),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.metavize.mvvm.tran.PipelineEndpoints
CREATE TABLE events.pl_endp (
    event_id int8 NOT NULL,
    time_stamp timestamp,
    session_id int4,
    proto int2,
    create_date timestamp,
    client_intf int2,
    server_intf int2,
    c_client_addr inet,
    s_client_addr inet,
    c_server_addr inet,
    s_server_addr inet,
    c_client_port int4,
    s_client_port int4,
    c_server_port int4,
    s_server_port int4,
    policy_id int8,
    policy_inbound bool,
    PRIMARY KEY (event_id));

-- com.metavize.mvvm.tran.PipelineStats
CREATE TABLE events.pl_stats (
    event_id int8 NOT NULL,
    time_stamp timestamp,
    pl_endp_id int8,
    raze_date timestamp,
    c2p_bytes int8,
    s2p_bytes int8,
    p2c_bytes int8,
    p2s_bytes int8,
    c2p_chunks int8,
    s2p_chunks int8,
    p2c_chunks int8,
    p2s_chunks int8,
    PRIMARY KEY (event_id));

-- com.metavize.mvvm.shield.ShieldRejectionEvent
CREATE TABLE events.shield_rejection_evt (
    event_id int8 NOT NULL,
    client_addr inet,
    client_intf int2,
    reputation float8,
    mode int4,
    limited int4,
    dropped int4,
    rejected int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.metavize.mvvm.shield.ShieldStatisticEvent
CREATE TABLE events.shield_statistic_evt (
    event_id int8 NOT NULL,
    accepted int4,
    limited  int4,
    dropped  int4,
    rejected int4,
    relaxed  int4,
    lax      int4,
    tight    int4,
    closed   int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

----------------
-- constraints |
----------------

-- indeces for reporting

CREATE INDEX pl_endp_sid_idx ON events.pl_endp (session_id);
CREATE INDEX pl_endp_cdate_idx ON events.pl_endp (create_date);
CREATE INDEX pl_stats_plepid_idx ON events.pl_stats (pl_endp_id);

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

ALTER TABLE events.pl_stats
    ADD CONSTRAINT fk_plstats_to_plendp
    FOREIGN KEY (pl_endp_id) REFERENCES events.pl_endp;
