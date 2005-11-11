-- converter for release 3.1

---------------------
-- point to pl_endp |
---------------------

DROP TABLE events.tr_mail_tmp;

CREATE TABLE events.tr_mail_tmp AS
    SELECT id, event_id AS pl_endp_id, subject::text, server_type
    FROM events.tr_mail_message_info JOIN events.pl_endp USING (session_id);

DROP TABLE events.tr_mail_message_info;
ALTER TABLE events.tr_mail_tmp RENAME TO tr_mail_message_info;
ALTER TABLE events.tr_mail_message_info ALTER COLUMN id SET NOT NULL;
ALTER TABLE events.tr_mail_message_info ALTER COLUMN subject SET NOT NULL;
ALTER TABLE events.tr_mail_message_info ALTER COLUMN server_type SET NOT NULL;
ALTER TABLE events.tr_mail_message_info ADD PRIMARY KEY (id);

-------------------
-- remove varchar |
-------------------

-- events.tr_mail_message_info_addr

DROP TABLE events.tr_mail_tmp;

CREATE TABLE events.tr_mail_tmp AS
    SELECT addr.id, addr::text, personal::text, kind, msg_id, position
    FROM events.tr_mail_message_info_addr addr
         JOIN events.tr_mail_message_info info ON addr.msg_id = info.id;

DROP TABLE events.tr_mail_message_info_addr;
ALTER TABLE events.tr_mail_tmp RENAME TO tr_mail_message_info_addr;
ALTER TABLE events.tr_mail_message_info_addr ALTER COLUMN id SET NOT NULL;
ALTER TABLE events.tr_mail_message_info_addr ALTER COLUMN addr SET NOT NULL;
ALTER TABLE events.tr_mail_message_info_addr ADD PRIMARY KEY (id);

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
    max_quarantine_sz int8 NOT NULL
    PRIMARY KEY (settings_id));

CREATE TABLE settings.tr_mail_safels_recipient (
    id int8 NOT NULL,
    addr text NOT NULL,
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

ALTER TABLE events.tr_mail_message_info_addr
    ADD CONSTRAINT fk_trml_msginfoaddr_to_msginfo
    FOREIGN KEY (msg_id)
    REFERENCES tr_mail_message_info;
