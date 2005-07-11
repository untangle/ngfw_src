-- schema for release-1.5

-- INITIAL VERSION -- NOT READY FOR PRIME TIME XXXXXXX

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
