-- events schema for release-5.0.3

-----------
-- events |
-----------

CREATE TABLE events.n_spam_evt_smtp (
    event_id int8 NOT NULL,
    msg_id int8,
    score float4,
    is_spam bool,
    action char(1),
    vendor_name varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE events.n_spam_evt (
    event_id int8 NOT NULL,
    msg_id int8,
    score float4,
    is_spam bool,
    action char(1),
    vendor_name varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE events.n_spam_smtp_tarpit_evt (
    event_id int8 NOT NULL,
    session_id int8 NOT NULL,
    ipaddr inet NOT NULL,
    hostname varchar(255) NOT NULL,
    vendor_name varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

----------------
-- constraints |
----------------

-- indices for reporting

CREATE INDEX n_spam_evt_smtp_ts_idx
    ON events.n_spam_evt_smtp (time_stamp);
CREATE INDEX n_spam_evt_ts_idx
    ON events.n_spam_evt (time_stamp);
CREATE INDEX n_spam_evt_mid_idx
    ON events.n_spam_evt (msg_id);
CREATE INDEX n_spam_evt_smtp_mid_idx
    ON events.n_spam_evt_smtp (msg_id);
CREATE INDEX n_spam_smtp_tarpit_evt_ts_idx
    ON events.n_spam_smtp_tarpit_evt (time_stamp);
