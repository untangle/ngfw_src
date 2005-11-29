-- events schema for release 3.1

-----------
-- events |
-----------

CREATE TABLE events.tr_mail_message_info (
    id int8 NOT NULL,
    pl_endp_id int8,
    subject text NOT NULL,
    server_type char(1) NOT NULL,
    PRIMARY KEY (id));

CREATE TABLE events.tr_mail_message_info_addr (
    id int8 NOT NULL,
    addr text NOT NULL,
    personal text,
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
ALTER TABLE events.tr_mail_message_info_addr
    ADD CONSTRAINT fk_trml_msginfoaddr_to_msginfo
    FOREIGN KEY (msg_id)
    REFERENCES tr_mail_message_info;
