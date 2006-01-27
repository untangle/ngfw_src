-- settings schema for release-3.2

-------------
-- settings |
-------------

-- com.metavize.tran.boxbackup.BoxBackupSettings
CREATE TABLE settings.tr_boxbackup_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    hour_in_day int4,
    minute_in_day int4,
    backup_url text,
    PRIMARY KEY (settings_id));

