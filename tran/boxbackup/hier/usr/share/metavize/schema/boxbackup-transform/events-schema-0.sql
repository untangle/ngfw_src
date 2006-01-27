-- events schema for release 3.2

-----------
-- events |
-----------

CREATE TABLE events.tr_boxbackup_evt (
    event_id int8 NOT NULL,
    success bool,
    description text,
    time_stamp timestamp,
    PRIMARY KEY (event_id));


----------------
-- constraints |
----------------

-- indices for reporting
CREATE INDEX tr_boxbackup_evt_ts_idx
    ON events.tr_boxbackup_evt (time_stamp);

