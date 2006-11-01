-- schema for release-2.5

-------------
-- settings |
-------------

-- com.untangle.tran.ftp.FtpSettings
CREATE TABLE settings.tr_ftp_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    enabled bool NOT NULL,
    PRIMARY KEY (settings_id));

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.tr_ftp_settings
    ADD CONSTRAINT fk_tr_ftp_settings_tid FOREIGN KEY (tid) REFERENCES tid;
