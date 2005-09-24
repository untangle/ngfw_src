-- schema for release-3.0

-------------
-- settings |
-------------

-- com.metavize.tran.ftp.FtpSettings
CREATE TABLE settings.tr_ftp_settings (
    settings_id int8 NOT NULL,
    enabled bool NOT NULL,
    PRIMARY KEY (settings_id));
