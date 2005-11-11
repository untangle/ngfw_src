-- schema for release after 3.0

-------------
-- settings |
-------------

CREATE TABLE settings.tr_mail_settings (
    settings_id int8 NOT NULL,
    smtp_enabled bool NOT NULL,
    pop_enabled bool NOT NULL,
    imap_enabled bool NOT NULL,
    smtp_inbound_timeout int8 NOT NULL,
    smtp_outbound_timeout int8 NOT NULL,
    pop_inbound_timeout int8 NOT NULL,
    pop_outbound_timeout int8 NOT NULL,
    imap_inbound_timeout int8 NOT NULL,
    imap_outbound_timeout int8 NOT NULL,
    quarantine_settings int8,
    PRIMARY KEY (settings_id));

CREATE TABLE settings.tr_mail_quarantine_settings (
    settings_id int8 NOT NULL,
    max_intern_time int8 NOT NULL,
    max_idle_inbox_time int8 NOT NULL,
    secret_key bytea NOT NULL,
    digest_from varchar(255) NOT NULL,
    hour_in_day int4,
    minute_in_day int4,
    max_quarantine_sz int8 NOT NULL,
    PRIMARY KEY (settings_id));

CREATE TABLE settings.tr_mail_safels_recipient (
    id int8 NOT NULL,
    addr varchar(255) NOT NULL,
    PRIMARY KEY (id));

CREATE TABLE settings.tr_mail_safels_sender (
    LIKE settings.tr_mail_safels_recipient,
    PRIMARY KEY (id));

-- com.metavize.tran.mail.papi.safelist.SafelistSettings
CREATE TABLE settings.tr_mail_safels_settings (
    safels_id int8 NOT NULL,
    recipient int8 NOT NULL,
    sender int8 NOT NULL,
    PRIMARY KEY (safels_id));

-- com.metavize.tran.mail.papi.MailTransformSettings.safelists (list construct)
CREATE TABLE settings.tr_mail_safelists (
    safels_id int8 NOT NULL,
    setting_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-----------
-- events |
-----------

CREATE TABLE events.tr_mail_message_info (
    id int8 NOT NULL,
    session_id int4,
    subject varchar(255) NOT NULL,
    server_type char(1) NOT NULL,
    PRIMARY KEY (id));

CREATE TABLE events.tr_mail_message_info_addr (
    id int8 NOT NULL,
    addr varchar(255) NOT NULL,
    personal varchar(255),
    kind char(1),
    msg_id int8,
    position int4,
    PRIMARY KEY (id));

CREATE TABLE events.tr_mail_message_stats (
    id int8 NOT NULL,
    msg_id int8,
    msg_bytes int8,
    msg_attachments int4,
    PRIMARY KEY (id));

----------------
-- constraints |
----------------

-- indices for reporting

CREATE INDEX tr_mail_mio_sid_idx ON events.tr_mail_message_info (session_id);

CREATE INDEX tr_mail_mioa_parent_idx ON events.tr_mail_message_info_addr (msg_id);

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
