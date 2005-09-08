-- com.metavize.mvvm.policy.Policy
CREATE TABLE settings.policy (
    id int8 NOT NULL,
    is_default bool NOT NULL,
    name varchar(255) NOT NULL,
    notes varchar(255),
    PRIMARY KEY (id));

INSERT INTO settings.policy
    VALUES (0, true, 'Default', 'The default policy');

-- com.metavize.mvvm.policy.UserPolicyRuleSet
CREATE TABLE settings.mvvm_user_policy_rules (
    set_id int8 NOT NULL,
    PRIMARY KEY (set_id));

INSERT INTO settings.mvvm_user_policy_rules
    VALUES (nextval('hibernate_sequence'));

-- com.metavize.mvvm.policy.UserPolicyRule
CREATE TABLE settings.user_policy_rule (
    rule_id int8 NOT NULL,
    protocol_matcher varchar(255),
    client_ip_matcher varchar(255),
    server_ip_matcher varchar(255),
    client_port_matcher varchar(255),
    server_port_matcher varchar(255),
    client_intf int2 NOT NULL,
    server_intf int2 NOT NULL,
    policy_id int8,
    is_inbound bool NOT NULL,
    name varchar(255),
    category varchar(255),
    description varchar(255),
    live bool,
    alert bool,
    log bool,
    set_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (rule_id));

-- com.metavize.mvvm.policy.SystemPolicyRule
CREATE TABLE settings.system_policy_rule (
    rule_id int8 NOT NULL,
    client_intf int2 NOT NULL,
    server_intf int2 NOT NULL,
    policy_id int8,
    is_inbound bool NOT NULL,
    name varchar(255),
    category varchar(255),
    description varchar(255),
    live bool,
    alert bool,
    log bool,
    PRIMARY KEY (rule_id));

INSERT INTO settings.system_policy_rule (
    SELECT nextval('hibernate_sequence'), 0, 1, id, true, '[no name]', '[no category]', '[no description]', true, false, false
    FROM settings.policy
);
INSERT INTO settings.system_policy_rule (
    SELECT nextval('hibernate_sequence'), 1, 0, id, false, '[no name]', '[no category]', '[no description]', true, false, false
    FROM settings.policy
);

-- com.metavize.mvvm.security.Tid
ALTER TABLE settings.tid ADD COLUMN policy_id int8;
UPDATE settings.tid SET policy_id = 0;
 
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
