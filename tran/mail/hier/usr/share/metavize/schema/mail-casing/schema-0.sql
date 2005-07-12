-- schema for release-2.5

-- INITIAL VERSION -- NOT READY FOR PRIME TIME XXXXXXX

CREATE TABLE tr_mail_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    smtp_enabled bool NOT NULL,
    pop_enabled bool NOT NULL,
    imap_enabled bool NOT NULL,
    PRIMARY KEY (settings_id));

CREATE TABLE email_message_info (
    id int8 NOT NULL,
    subject varchar(255) NOT NULL,
    PRIMARY KEY (id));

CREATE TABLE email_message_info_addr (
    id int8 NOT NULL,
    addr varchar(255) NOT NULL,
    personal varchar(255),
    kind char,
    PRIMARY KEY (id));

CREATE TABLE email_message_stats (
    id int8 NOT NULL,
    msg_id int8 NOT NULL,
    long msg_bytes,
    int msg_attachments,
    PRIMARY KEY (id));

ALTER TABLE tr_mail_settings ADD CONSTRAINT tr_mail_settings_tid_fk FOREIGN KEY (tid) REFERENCES tid;
