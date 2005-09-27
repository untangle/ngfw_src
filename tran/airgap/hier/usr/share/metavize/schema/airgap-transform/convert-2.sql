ALTER TABLE  settings.tr_airgap_settings RENAME COLUMN id TO settings_id;

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
