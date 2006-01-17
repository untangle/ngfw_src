-- events schema for release 3.1.3

-----------
-- tables |
-----------

CREATE TABLE events.tr_mail_message_info (
    id int8 NOT NULL,
    pl_endp_id int8,
    subject text NOT NULL,
    server_type char(1) NOT NULL,
    time_stamp timestamp,
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

-- indexes for reporting

CREATE INDEX tr_mail_mio_plepid_idx ON events.tr_mail_message_info (pl_endp_id);
CREATE INDEX tr_mail_mioa_parent_idx ON events.tr_mail_message_info_addr (msg_id);
