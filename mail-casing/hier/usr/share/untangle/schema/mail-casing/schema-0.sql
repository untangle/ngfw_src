-- schema for release-2.5

-- INITIAL VERSION -- NOT READY FOR PRIME TIME XXXXXXX

-------------
-- settings |
-------------

CREATE TABLE settings.tr_mail_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    smtp_enabled bool NOT NULL,
    pop_enabled bool NOT NULL,
    imap_enabled bool NOT NULL,
    smtp_inbound_timeout int8 NOT NULL,
    smtp_outbound_timeout int8 NOT NULL,
    pop_inbound_timeout int8 NOT NULL,
    pop_outbound_timeout int8 NOT NULL,
    imap_inbound_timeout int8 NOT NULL,
    imap_outbound_timeout int8 NOT NULL,
    PRIMARY KEY (settings_id));

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

-- indeces for reporting

CREATE INDEX tr_mail_mio_sid_idx ON events.tr_mail_message_info (session_id);

-- foreign key constraints

ALTER TABLE settings.tr_mail_settings
    ADD CONSTRAINT fk_tr_mail_settings_tid FOREIGN KEY (tid) REFERENCES tid;
