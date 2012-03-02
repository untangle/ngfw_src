
CREATE TABLE events.n_mail_message_info (
    id int8 NOT NULL,
    session_id int8,
    subject text NOT NULL,
    server_type char(1) NOT NULL,
    time_stamp timestamp,
    PRIMARY KEY (id));

CREATE TABLE events.n_mail_message_info_addr (
    id int8 NOT NULL,
    addr text NOT NULL,
    personal text,
    kind char(1),
    msg_id int8,
    position int4,
    time_stamp timestamp,
    PRIMARY KEY (id));

CREATE INDEX n_mail_message_info_session_id_idx ON events.n_mail_message_info (session_id);
CREATE INDEX n_mail_message_info_addr_msg_id_idx ON events.n_mail_message_info_addr (msg_id);
