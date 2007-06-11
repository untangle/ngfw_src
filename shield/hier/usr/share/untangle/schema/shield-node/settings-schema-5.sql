-- settings schema for release-5.0

-------------
-- settings |
-------------

-- com.untangle.tran.airgap.AirgapSettings
CREATE TABLE settings.n_shield_settings (
    settings_id int8 NOT NULL,
    tid         int8 NOT NULL UNIQUE,
    PRIMARY KEY (settings_id));

CREATE TABLE settings.n_shield_node_rule (
    rule_id     INT8 NOT NULL,
    name        text,
    category    text,
    description text,
    live        BOOL NOT NULL,
    alert       BOOL NOT NULL,
    log         BOOL NOT NULL,
    address     INET,
    netmask     INET,
    divider     REAL NOT NULL,
    settings_id INT8,
    position    INT4,
    PRIMARY KEY (rule_id));

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.n_shield_settings
    ADD CONSTRAINT fk_tr_airgap_settings FOREIGN KEY (tid) REFERENCES u_tid;

ALTER TABLE settings.n_shield_node_rule
    ADD CONSTRAINT fk_tr_airgap_shield_node_rule
        FOREIGN KEY (settings_id) REFERENCES settings.n_shield_settings;
