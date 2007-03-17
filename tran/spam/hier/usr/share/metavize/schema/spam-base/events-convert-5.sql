-- events convert for release 4.2

-----------
-- events |
-----------

CREATE TABLE events.tr_spam_smtp_rbl_evt (
    event_id int8 NOT NULL,
    hostname varchar(255) NOT NULL,
    ipaddr inet NOT NULL,
    skipped bool NOT NULL,
    pl_endp_id int8 NOT NULL,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- BEGIN dirty hack, make settings.tr_clamphish_settings
CREATE TABLE settings.tr_clamphish_settings (
    spam_settings_id int8 NOT NULL,
    enable_google_sb bool NOT NULL,
    PRIMARY KEY (settings_id));

ALTER TABLE settings.tr_clamphish_settings
    ADD CONSTRAINT fk_clamphish_to_spam_settings
    FOREIGN KEY (spam_settings_id)
    REFERENCES settings.tr_spam_settings;
-- END dirty hack, make settings.tr_clamphish_settings

----------------
-- constraints |
----------------

-- indices for reporting

CREATE INDEX tr_spam_smtp_rbl_evt_ts_idx
    ON events.tr_spam_smtp_rbl_evt (time_stamp);
