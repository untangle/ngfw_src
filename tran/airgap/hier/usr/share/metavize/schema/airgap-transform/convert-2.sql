ALTER TABLE  settings.tr_airgap_settings RENAME COLUMN id TO settings_id;

CREATE TABLE settings.tr_airgap_shield_node_rule (
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

ALTER TABLE settings.tr_airgap_shield_node_rule
    ADD CONSTRAINT fk_tr_airgap_shield_node_rule
        FOREIGN KEY (settings_id) REFERENCES settings.tr_airgap_settings;
