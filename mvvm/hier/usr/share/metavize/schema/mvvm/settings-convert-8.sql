-- settings conversion for release-4.1

ALTER TABLE settings.mvvm_user ADD COLUMN email text;

-- com.metavize.mvvm.networking.PPPoEConnectionRule -- 4.1
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
    PRIMARY KEY       (rule_id));

-- com.metavize.mvvm.networking.PPPoESettings -- 4.1
CREATE TABLE settings.mvvm_pppoe (
    settings_id       INT8 NOT NULL,
    live              BOOL,
    PRIMARY KEY      (settings_id));
