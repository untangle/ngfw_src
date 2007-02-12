-- events schema for release 4.2

-----------
-- events |
-----------

CREATE TABLE events.tr_spam_evt_smtp (
    event_id int8 NOT NULL,
    msg_id int8,
    score float4,
    is_spam bool,
    action char(1),
    vendor_name varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE events.tr_spam_evt (
    event_id int8 NOT NULL,
    msg_id int8,
    score float4,
    is_spam bool,
    action char(1),
    vendor_name varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE events.tr_spam_smtp_rbl_evt (
    event_id int8 NOT NULL,
    hostname varchar(255) NOT NULL,
    ipaddr inet NOT NULL,
    skipped bool NOT NULL,
    pl_endp_id int8 NOT NULL,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

----------------
-- constraints |
----------------

-- indices for reporting

CREATE INDEX tr_spam_evt_smtp_ts_idx
    ON events.tr_spam_evt_smtp (time_stamp);
CREATE INDEX tr_spam_evt_ts_idx
    ON events.tr_spam_evt (time_stamp);
CREATE INDEX tr_spam_evt_mid_idx
    ON events.tr_spam_evt (msg_id);
CREATE INDEX tr_spam_evt_smtp_mid_idx
    ON events.tr_spam_evt_smtp (msg_id);
CREATE INDEX tr_spam_smtp_rbl_evt_ts_idx
    ON events.tr_spam_smtp_rbl_evt (time_stamp);
