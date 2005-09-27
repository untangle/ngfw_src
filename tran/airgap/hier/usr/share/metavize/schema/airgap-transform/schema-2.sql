-- schema for release-3.0

-------------
-- settings |
-------------

-- com.metavize.tran.airgap.AirgapSettings
CREATE TABLE settings.tr_airgap_settings (
    settings_id int8 NOT NULL,
    tid         int8 NOT NULL UNIQUE,
    PRIMARY KEY (settings_id));

CREATE TABLE settings.shield_node_rule (
    rule_id     INT8 NOT NULL,
    name        VARCHAR(255),
    category    VARCHAR(255),
    description VARCHAR(255),
    live        bool,
    alert       bool,
    log         bool,
    address     INET,
    netmask     INET,
    divider     REAL,
    settings_id int8,
    position    int4,
    PRIMARY KEY (rule_id)); 

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.tr_airgap_settings
    ADD CONSTRAINT fk_tr_airgap_settings FOREIGN KEY (tid) REFERENCES tid;
