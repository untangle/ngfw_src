-- settings schema for release-5.0

-------------
-- settings |
-------------

-- com.untangle.tran.firewall.FirewallRule
CREATE TABLE settings.n_firewall_rule (
    rule_id int8 NOT NULL,
    settings_id int8,
    position int4,
    is_traffic_blocker bool,
    protocol_matcher text,
    src_ip_matcher text,
    dst_ip_matcher text,
    src_port_matcher text,
    dst_port_matcher text,
    inbound bool,
    outbound bool,
    name text,
    category text,
    description text,
    live bool,
    alert bool,
    log bool,
    PRIMARY KEY (rule_id));

-- com.untangle.tran.firewall.FirewallSettings
CREATE TABLE settings.n_firewall_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    is_quickexit BOOL,
    is_reject_silent BOOL,
    is_default_accept BOOL,
    PRIMARY KEY (settings_id));

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.n_firewall_settings
    ADD CONSTRAINT fk_tr_firewall_settings
        FOREIGN KEY (tid) REFERENCES settings.u_tid;
