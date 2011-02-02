-- settings conversion for release-4.1
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

ALTER TABLE settings.mvvm_user ADD COLUMN email text;

-- com.untangle.mvvm.networking.PPPoEConnectionRule -- 4.1
CREATE TABLE settings.mvvm_pppoe_connection (
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

-- com.untangle.mvvm.networking.PPPoESettings -- 4.1
CREATE TABLE settings.mvvm_pppoe (
    settings_id       INT8 NOT NULL,
    live              BOOL,
    PRIMARY KEY      (settings_id));

-- com.untangle.mvvm.user.WMISettings -- 4.1
CREATE TABLE settings.mvvm_wmi_settings (
    settings_id       INT8 NOT NULL,
    live              BOOL,
    scheme            TEXT,
    address           INET,
    port              INT4,
    username          TEXT,
    password          TEXT,
    PRIMARY KEY       (settings_id));

-- com.untangle.mvvm.policy.UserPolicyRule
DROP TABLE settings.policy_tmp;

CREATE TABLE settings.policy_tmp AS
    SELECT rule_id, protocol_matcher, client_ip_matcher, server_ip_matcher,
             client_port_matcher, server_port_matcher,
        case client_intf when 0 then 'O' when 1 then 'I' when 2 then 'D' when 3 then 'V' end as client_intf_matcher,
        case server_intf when 0 then 'O' when 1 then 'I' when 2 then 'D' when 3 then 'V' end as server_intf_matcher,
        policy_id, is_inbound, name, category, description, live, alert, log, set_id,
        position, time '00:00:00' as start_time, time '23:59:00' as end_time, 'all'::text as day_of_week_matcher, 'all'::text as user_matcher, false as invert_entire_duration
    FROM settings.user_policy_rule;

DROP TABLE settings.user_policy_rule;
ALTER TABLE settings.policy_tmp RENAME TO user_policy_rule;
ALTER TABLE settings.user_policy_rule ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.user_policy_rule ALTER COLUMN is_inbound SET NOT NULL;
ALTER TABLE settings.user_policy_rule ALTER COLUMN invert_entire_duration SET NOT NULL;
ALTER TABLE settings.user_policy_rule ADD PRIMARY KEY (rule_id);

ALTER TABLE settings.user_policy_rule
    ADD CONSTRAINT fk_user_policy_rule_parent
    FOREIGN KEY (set_id) REFERENCES settings.mvvm_user_policy_rules;

ALTER TABLE settings.user_policy_rule
    ADD CONSTRAINT fk_user_policy_rule_policy
    FOREIGN KEY (policy_id) REFERENCES settings.policy;

UPDATE settings.portal_home_settings
    SET home_page_text='Welcome to the Untangle Remote Access Portal'
     WHERE home_page_text like '%Metavize%';
