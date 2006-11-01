-- settings schema for release-3.2

-------------
-- settings |
-------------

-- com.untangle.tran.reporting.ReportingSettings
CREATE TABLE settings.tr_reporting_settings (
    id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    network_directory int8 NOT NULL,
    PRIMARY KEY (id));

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.tr_reporting_settings
    ADD CONSTRAINT fk_tr_reporting_settings
    FOREIGN KEY (tid) REFERENCES settings.tid;

ALTER TABLE settings.tr_reporting_settings
    ADD CONSTRAINT fk_tr_reporting_settings_to_ipmaddr_dir
    FOREIGN KEY (network_directory) REFERENCES settings.ipmaddr_dir;
