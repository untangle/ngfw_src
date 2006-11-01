-- convert script for release 2.5

-----------------------------------
-- move old tables to new schemas |
-----------------------------------

-- com.untangle.tran.firewall.FirewallRule (removing is_dst_redirect)
CREATE TABLE settings.firewall_rule (
    rule_id,
    is_traffic_blocker,
    protocol_matcher,
    src_ip_matcher,
    dst_ip_matcher,
    src_port_matcher,
    dst_port_matcher,
    src_intf_matcher,
    dst_intf_matcher,
    name,
    category,
    description,
    live,
    alert,
    log)
AS SELECT rule_id, is_traffic_blocker, protocol_matcher, src_ip_matcher,
          dst_ip_matcher, src_port_matcher, dst_port_matcher, src_intf_matcher,
          dst_intf_matcher, name, category, description, live, alert, log
   FROM public.firewall_rule;

ALTER TABLE settings.firewall_rule
    ADD CONSTRAINT firewall_rule_pkey PRIMARY KEY (rule_id);
ALTER TABLE settings.firewall_rule
    ALTER COLUMN rule_id SET NOT NULL;

-- com.untangle.tran.firewall.FirewallSettings.firewallRuleList
CREATE TABLE settings.tr_firewall_rules
    AS SELECT * FROM public.tr_firewall_rules;

ALTER TABLE settings.tr_firewall_rules
    ADD CONSTRAINT tr_firewall_rules_pkey PRIMARY KEY (setting_id, position);
ALTER TABLE settings.tr_firewall_rules
    ALTER COLUMN setting_id SET NOT NULL;
ALTER TABLE settings.tr_firewall_rules
    ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.tr_firewall_rules
    ALTER COLUMN position SET NOT NULL;

-- com.untangle.tran.firewall.FirewallSettings
CREATE TABLE settings.tr_firewall_settings
    AS SELECT * FROM public.tr_firewall_settings;

ALTER TABLE settings.tr_firewall_settings
    ADD CONSTRAINT tr_firewall_settings_pkey PRIMARY KEY (settings_id);
ALTER TABLE settings.tr_firewall_settings
    ADD CONSTRAINT tr_firewall_settings_uk UNIQUE (tid);
ALTER TABLE settings.tr_firewall_settings
    ALTER COLUMN settings_id SET NOT NULL;
ALTER TABLE settings.tr_firewall_settings
    ALTER COLUMN tid SET NOT NULL;

-------------------------
-- recreate constraints |
-------------------------

-- foreign key constraints

ALTER TABLE settings.tr_firewall_rules
    ADD CONSTRAINT fk_tr_firewall_rules
        FOREIGN KEY (rule_id) REFERENCES settings.firewall_rule;
ALTER TABLE settings.tr_firewall_rules
    ADD CONSTRAINT fk_tr_firewall_rules_settings
        FOREIGN KEY (setting_id) REFERENCES settings.tr_firewall_settings;
ALTER TABLE settings.tr_firewall_settings
    ADD CONSTRAINT fk_tr_firewall_settings FOREIGN KEY (tid) REFERENCES settings.tid;

-------------------------
-- drop old constraints |
-------------------------

-- foreign key constraints

ALTER TABLE tr_firewall_rules DROP CONSTRAINT fk4bbfb8b9871aad3e;
ALTER TABLE tr_firewall_rules DROP CONSTRAINT fk4bbfb8b91cae658a;
ALTER TABLE tr_firewall_settings DROP CONSTRAINT fk23cda1011446f;

--------------------
-- drop old tables |
--------------------

DROP TABLE public.firewall_rule;
DROP TABLE public.tr_firewall_rules;
DROP TABLE public.tr_firewall_settings;

---------------
-- new tables |
---------------

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

-- indeces for reporting

CREATE INDEX tr_firewall_evt_sid_idx ON events.tr_firewall_evt (session_id);
