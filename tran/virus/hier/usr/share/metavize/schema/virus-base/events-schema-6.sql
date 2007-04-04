-- events schema for release 3.1

-----------
-- events |
-----------

CREATE TABLE events.tr_virus_evt (
    event_id int8 NOT NULL,
    pl_endp_id int8,
    clean bool,
    virus_name text,
    virus_cleaned bool,
    vendor_name text,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE events.tr_virus_evt_http (
    event_id int8 NOT NULL,
    request_line int8,
    clean bool,
    virus_name text,
    virus_cleaned bool,
    vendor_name text,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE events.tr_virus_evt_smtp (
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

CREATE TABLE events.tr_virus_evt_mail (
    event_id int8 NOT NULL,
    msg_id int8,
    clean bool,
    virus_name text,
    virus_cleaned bool,
    action char(1),
    vendor_name text,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

----------------
-- constraints |
----------------

-- indices for reporting

CREATE INDEX tr_virus_evt_http_rid_idx ON events.tr_virus_evt_http (request_line);

CREATE INDEX tr_virus_evt_http_ts_idx ON events.tr_virus_evt_http (time_stamp);
CREATE INDEX tr_virus_evt_ts_idx ON events.tr_virus_evt (time_stamp);
CREATE INDEX tr_virus_evt_smtp_ts_idx ON events.tr_virus_evt_smtp (time_stamp);
CREATE INDEX tr_virus_evt_mail_ts_idx ON events.tr_virus_evt_mail (time_stamp);
CREATE INDEX tr_virus_evt_smtp_mid_idx ON events.tr_virus_evt_smtp (msg_id);
CREATE INDEX tr_virus_evt_mail_mid_idx ON events.tr_virus_evt_mail (msg_id);

