-- converter to version 3.0

ALTER TABLE settings.tr_mail_settings
    ADD COLUMN quarantine_settings int8;

CREATE TABLE settings.tr_mail_quarantine_settings (
    settings_id int8 NOT NULL,
    max_intern_time int8 NOT NULL,
    max_idle_inbox_time int8 NOT NULL,
    secret_key bytea NOT NULL,
    digest_from varchar(255) NOT NULL,
    hour_in_day int4,
    max_quarantine_sz int8 NOT NULL
    PRIMARY KEY (settings_id));
