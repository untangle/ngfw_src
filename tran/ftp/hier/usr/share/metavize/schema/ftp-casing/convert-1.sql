-- convert script for release 3.0

ALTER TABLE tr_ftp_settings
    DROP CONSTRAINT fk_tr_ftp_settings_tid;

ALTER TABLE tr_ftp_settings
    DROP COLUMN tid;