-- events convert for release 4.2

-----------
-- events |
-----------

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

CREATE INDEX tr_spam_smtp_rbl_evt_ts_idx
    ON events.tr_spam_smtp_rbl_evt (time_stamp);
