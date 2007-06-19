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

-- com.untangle.mvvm.policy.Policy
CREATE TABLE settings.policy (
    id int8 NOT NULL,
    is_default bool NOT NULL,
    name varchar(255) NOT NULL,
    notes varchar(255),
    PRIMARY KEY (id));

INSERT INTO settings.policy
    VALUES (0, true, 'Default Rack', 'The default rack');

-- com.untangle.mvvm.policy.UserPolicyRuleSet
CREATE TABLE settings.mvvm_user_policy_rules (
    set_id int8 NOT NULL,
    PRIMARY KEY (set_id));

INSERT INTO settings.mvvm_user_policy_rules
    VALUES (nextval('hibernate_sequence'));

-- com.untangle.mvvm.policy.UserPolicyRule
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
    set_id int8,
    position int4,
    PRIMARY KEY (rule_id));

-- com.untangle.mvvm.policy.SystemPolicyRule
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

-- com.untangle.mvvm.security.Tid
ALTER TABLE settings.tid ADD COLUMN policy_id int8;
UPDATE settings.tid SET policy_id = 0 FROM
 (SELECT tps.tid FROM transform_persistent_state tps WHERE tps.name NOT LIKE '%-casing' AND tps.name NOT IN ('nat-transform', 'reporting-transform', 'airgap-transform')) AS foo
 WHERE id = foo.tid;

-- com.untangle.mvvm.tran.PipelineEndpoints
DROP TABLE events.pl_endp_new;
CREATE TABLE events.pl_endp_new (
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
    policy_inbound bool);

INSERT INTO events.pl_endp_new
SELECT DISTINCT event_id, time_stamp, session_id, proto, create_date, client_intf, server_intf, c_client_addr,
       s_client_addr, c_server_addr, s_server_addr, c_client_port, s_client_port, c_server_port, s_server_port,
       0, CASE WHEN client_intf = 0 THEN true ELSE false END
  FROM events.pl_endp;

DROP INDEX pl_endp_sid;
DROP INDEX pl_endp_sid_idx;
DROP INDEX pl_endp_cdate_idx;
DROP TABLE events.pl_endp;

ALTER TABLE events.pl_endp_new RENAME TO pl_endp;
ALTER TABLE events.pl_endp
    ADD CONSTRAINT pl_endp_pkey PRIMARY KEY (event_id);
CREATE INDEX pl_endp_sid_idx ON events.pl_endp (session_id);
CREATE INDEX pl_endp_cdate_idx ON events.pl_endp (create_date);

-- com.untangle.mvvm.MailSettings
ALTER TABLE settings.mail_settings ADD COLUMN smtp_port int4;
ALTER TABLE settings.mail_settings ADD COLUMN use_tls bool;
ALTER TABLE settings.mail_settings ADD COLUMN auth_user varchar(255);
ALTER TABLE settings.mail_settings ADD COLUMN auth_pass varchar(255);
ALTER TABLE settings.mail_settings ADD COLUMN local_host_name varchar(255);
UPDATE settings.mail_settings SET smtp_port = 25;
UPDATE settings.mail_settings SET use_tls = false;
ALTER TABLE settings.mail_settings ALTER COLUMN smtp_port SET NOT NULL;
ALTER TABLE settings.mail_settings ALTER COLUMN use_tls SET NOT NULL;


-- Constraints
 
CREATE INDEX pl_stats_sid_idx ON events.pl_stats (session_id);

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
