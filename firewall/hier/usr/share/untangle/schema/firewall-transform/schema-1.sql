-- schema for release 2.5

-------------
-- settings |
-------------

-- com.untangle.tran.firewall.FirewallRule
CREATE TABLE settings.firewall_rule (
    rule_id int8 NOT NULL,
    is_traffic_blocker bool,
    protocol_matcher varchar(255),
    src_ip_matcher varchar(255),
    dst_ip_matcher varchar(255),
    src_port_matcher varchar(255),
    dst_port_matcher varchar(255),
    src_intf_matcher varchar(255),
    dst_intf_matcher varchar(255),
    name varchar(255),
    category varchar(255),
    description varchar(255),
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
    is_quickexit bool,
    is_reject_silent bool,
    is_default_accept bool,
    PRIMARY KEY (settings_id));

-----------
-- events |
-----------

-- com.untangle.tran.firewall.FirewallEvent
CREATE TABLE events.tr_firewall_evt (
    event_id int8 NOT NULL,
    session_id int4,
    rule_id int8,
    rule_index int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.firewall.FirewallStatisticEvent
CREATE TABLE events.tr_firewall_statistic_evt (
    event_id int8 NOT NULL,
    tcp_block_default int4,
    tcp_block_rule int4,
    tcp_pass_default int4,
    tcp_pass_rule int4,
    udp_block_default int4,
    udp_block_rule int4,
    udp_pass_default int4,
    udp_pass_rule int4,
    icmp_block_default int4,
    icmp_block_rule int4,
    icmp_pass_default int4,
    icmp_pass_rule int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

----------------
-- constraints |
----------------

-- indeces for reporting

CREATE INDEX tr_firewall_evt_sid_idx ON events.tr_firewall_evt (session_id);

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
