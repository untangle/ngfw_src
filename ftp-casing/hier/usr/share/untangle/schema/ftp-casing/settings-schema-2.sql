-- settings schema for release-5.0

-- com.untangle.tran.ftp.FtpSettings
CREATE TABLE settings.n_ftp_settings (
    settings_id int8 NOT NULL,
    enabled bool NOT NULL,
    PRIMARY KEY (settings_id));
