-- settings conversion for release-3.2

-- drop incorrect constraint from 3.1 (which used network_directory to reference ipmaddr_dir table)
ALTER TABLE settings.tr_reporting_settings
    DROP CONSTRAINT fk_tr_reporting_settings;

-- recreate (e.g., correctly create) constraint (which uses tid to reference tid table)
ALTER TABLE settings.tr_reporting_settings
    ADD CONSTRAINT fk_tr_reporting_settings
    FOREIGN KEY (tid) REFERENCES settings.tid;

-- create constraint (which uses network_directory to reference ipmaddr_dir table)
ALTER TABLE settings.tr_reporting_settings
    ADD CONSTRAINT fk_tr_reporting_settings_to_ipmaddr_dir
    FOREIGN KEY (network_directory) REFERENCES settings.ipmaddr_dir;
