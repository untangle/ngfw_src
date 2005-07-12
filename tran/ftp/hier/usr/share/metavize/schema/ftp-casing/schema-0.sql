-- schema for release-2.5

CREATE TABLE tr_ftp_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    enabled bool NOT NULL,
    PRIMARY KEY (settings_id));

ALTER TABLE tr_ftp_settings ADD CONSTRAINT tr_ftp_settings_tid_fk FOREIGN KEY (tid) REFERENCES tid;
