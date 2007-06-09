-- schema for release-3.0

-------------
-- settings |
-------------

-- com.untangle.tran.airgap.AirgapSettings
CREATE TABLE settings.tr_airgap_settings (
    settings_id int8 NOT NULL,
    tid         int8 NOT NULL UNIQUE,
    PRIMARY KEY (settings_id));

CREATE TABLE settings.tr_airgap_shield_node_rule (
    rule_id     INT8 NOT NULL,
    name        VARCHAR(255),
    category    VARCHAR(255),
    description VARCHAR(255),
    live        BOOL,
    alert       BOOL,
    log         BOOL,
    address     INET,
    netmask     INET,
    divider     REAL,
    settings_id INT8,
    position    INT4,
    PRIMARY KEY (rule_id)); 

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.tr_airgap_settings
    ADD CONSTRAINT fk_tr_airgap_settings FOREIGN KEY (tid) REFERENCES tid;

ALTER TABLE settings.tr_airgap_shield_node_rule
    ADD CONSTRAINT fk_tr_airgap_shield_node_rule
        FOREIGN KEY (settings_id) REFERENCES settings.tr_airgap_settings;
