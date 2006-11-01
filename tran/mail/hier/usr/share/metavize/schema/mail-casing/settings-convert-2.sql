-- converter for release 3.1

---------------
-- quarantine |
---------------

ALTER TABLE settings.tr_mail_settings
    ADD COLUMN quarantine_settings int8;

CREATE TABLE settings.tr_mail_quarantine_settings (
    settings_id int8 NOT NULL,
    max_intern_time int8 NOT NULL,
    max_idle_inbox_time int8 NOT NULL,
    secret_key bytea NOT NULL,
    digest_from text NOT NULL,
    hour_in_day int4,
    minute_in_day int4,
    max_quarantine_sz int8 NOT NULL,
    PRIMARY KEY (settings_id));

CREATE TABLE settings.tr_mail_safels_recipient (
    id int8 NOT NULL,
    addr text NOT NULL,
    PRIMARY KEY (id));

CREATE TABLE settings.tr_mail_safels_sender (
    LIKE settings.tr_mail_safels_recipient,
    PRIMARY KEY (id));

-- com.untangle.tran.mail.papi.safelist.SafelistSettings
CREATE TABLE settings.tr_mail_safels_settings (
    safels_id int8 NOT NULL,
    recipient int8 NOT NULL,
    sender int8 NOT NULL,
    PRIMARY KEY (safels_id));

-- com.untangle.tran.mail.papi.MailTransformSettings.safelists (list construct)
CREATE TABLE settings.tr_mail_safelists (
    safels_id int8 NOT NULL,
    setting_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

----------------
-- constraints |
----------------

-- foreign key constraints
ALTER TABLE settings.tr_mail_safelists
    ADD CONSTRAINT fk_trml_safelists_to_ml_settings
    FOREIGN KEY (setting_id)
    REFERENCES settings.tr_mail_settings;

ALTER TABLE settings.tr_mail_safelists
    ADD CONSTRAINT fk_trml_safelists_to_sl_settings
    FOREIGN KEY (safels_id)
    REFERENCES settings.tr_mail_safels_settings;

ALTER TABLE settings.tr_mail_safels_settings
    ADD CONSTRAINT fk_trml_sl_settings_to_sl_recipient
    FOREIGN KEY (recipient)
    REFERENCES settings.tr_mail_safels_recipient;

ALTER TABLE settings.tr_mail_safels_settings
    ADD CONSTRAINT fk_trml_sl_settings_to_sl_sender
    FOREIGN KEY (sender)
    REFERENCES settings.tr_mail_safels_sender;
