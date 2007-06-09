-- schema for release 2.5.3

-------------
-- settings |
-------------

CREATE TABLE settings.tr_virus_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    disable_ftp_resume bool,
    disable_http_resume bool,
    trickle_percent int4,
    http_inbound int8 NOT NULL,
    http_outbound int8 NOT NULL,
    ftp_inbound int8 NOT NULL,
    ftp_outbound int8 NOT NULL,
    smtp_inbound int8 NOT NULL,
    smtp_outbound int8 NOT NULL,
    pop_inbound int8 NOT NULL,
    pop_outbound int8 NOT NULL,
    imap_inbound int8 NOT NULL,
    imap_outbound int8 NOT NULL,
    ftp_disable_resume_details varchar(255),
    http_disable_resume_details varchar(255),
    trickle_percent_details varchar(255),
    PRIMARY KEY (settings_id));

CREATE TABLE settings.tr_virus_vs_ext (
    settings_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

CREATE TABLE settings.tr_virus_config (
    config_id int8 NOT NULL,
    scan bool,
    copy_on_block bool,
    notes varchar(255),
    copy_on_block_notes varchar(255),
    PRIMARY KEY (config_id));

CREATE TABLE settings.tr_virus_smtp_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    action char(1) NOT NULL,
    notify_action char(1) NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

CREATE TABLE settings.tr_virus_pop_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    action char(1) NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

CREATE TABLE settings.tr_virus_imap_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    action char(1) NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

CREATE TABLE settings.tr_virus_vs_mt (
    settings_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

-----------
-- events |
-----------

CREATE TABLE events.tr_virus_evt (
    event_id int8 NOT NULL,
    session_id int4,
    clean bool,
    virus_name varchar(255),
    virus_cleaned bool,
    vendor_name varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE events.tr_virus_evt_http (
    event_id int8 NOT NULL,
    request_line int8,
    clean bool,
    virus_name varchar(255),
    virus_cleaned bool,
    vendor_name varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE events.tr_virus_evt_smtp (
    event_id int8 NOT NULL,
    msg_id int8,
    clean bool,
    virus_name varchar(255),
    virus_cleaned bool,
    action char(1),
    notify_action char(1),
    vendor_name varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE events.tr_virus_evt_mail (
    event_id int8 NOT NULL,
    msg_id int8,
    clean bool,
    virus_name varchar(255),
    virus_cleaned bool,
    action char(1),
    vendor_name varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

----------------
-- constraints |
----------------

-- indeces for reporting

CREATE INDEX tr_virus_evt_sid_idx
    ON events.tr_virus_evt (session_id);
CREATE INDEX tr_virus_evt_http_rid_idx
    ON events.tr_virus_evt_http (request_line);
CREATE INDEX tr_virus_evt_mail_mid_idx
    ON events.tr_virus_evt_mail (msg_id);
CREATE INDEX tr_virus_evt_smtp_mid_idx
    ON evnets.tr_virus_evt_smtp (msg_id);

-- foreign key constraints

ALTER TABLE settings.tr_virus_vs_ext
    ADD CONSTRAINT fk_tr_virus_vs_ext
    FOREIGN KEY (settings_id)
    REFERENCES settings.tr_virus_settings;

ALTER TABLE settings.tr_virus_vs_mt
    ADD CONSTRAINT fk_tr_virus_vs_mt
    FOREIGN KEY (settings_id)
    REFERENCES settings.tr_virus_settings;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings
    FOREIGN KEY (tid)
    REFERENCES settings.tid;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_ftpout
    FOREIGN KEY (ftp_outbound)
    REFERENCES settings.tr_virus_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_ftpin
    FOREIGN KEY (ftp_inbound)
    REFERENCES settings.tr_virus_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_httpout
    FOREIGN KEY (http_outbound)
    REFERENCES settings.tr_virus_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_set_httpin
    FOREIGN KEY (http_inbound)
    REFERENCES settings.tr_virus_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_smtpout
    FOREIGN KEY (smtp_outbound)
    REFERENCES settings.tr_virus_smtp_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_smtpin
    FOREIGN KEY (smtp_inbound)
    REFERENCES settings.tr_virus_smtp_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_popout
    FOREIGN KEY (pop_outbound)
    REFERENCES settings.tr_virus_pop_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_popin
    FOREIGN KEY (pop_inbound)
    REFERENCES settings.tr_virus_pop_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_imapout
    FOREIGN KEY (imap_outbound)
    REFERENCES settings.tr_virus_imap_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_imapin
    FOREIGN KEY (imap_inbound)
    REFERENCES settings.tr_virus_imap_config;
