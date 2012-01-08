CREATE TABLE events.n_virus_evt (
    event_id int8 NOT NULL,
    session_id int8,
    clean bool,
    virus_name text,
    virus_cleaned bool,
    vendor_name text,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE events.n_virus_evt_http (
    event_id int8 NOT NULL,
    request_line int8,
    clean bool,
    virus_name text,
    virus_cleaned bool,
    vendor_name text,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE events.n_virus_evt_smtp (
    event_id int8 NOT NULL,
    msg_id int8,
    clean bool,
    virus_name text,
    virus_cleaned bool,
    action char(1),
    notify_action char(1),
    vendor_name text,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE events.n_virus_evt_mail (
    event_id int8 NOT NULL,
    msg_id int8,
    clean bool,
    virus_name text,
    virus_cleaned bool,
    action char(1),
    vendor_name text,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE INDEX n_virus_evt_http_request_line_idx ON events.n_virus_evt_http (request_line);
CREATE INDEX n_virus_evt_http_time_stamp_idx ON events.n_virus_evt_http (time_stamp);
CREATE INDEX n_virus_evt_time_stamp_idx ON events.n_virus_evt (time_stamp);
CREATE INDEX n_virus_evt_smtp_time_stamp_idx ON events.n_virus_evt_smtp (time_stamp);
CREATE INDEX n_virus_evt_mail_time_stamp_idx ON events.n_virus_evt_mail (time_stamp);
CREATE INDEX n_virus_evt_smtp_msg_id_idx ON events.n_virus_evt_smtp (msg_id);
CREATE INDEX n_virus_evt_mail_msg_id_idx ON events.n_virus_evt_mail (msg_id);

