-- events schema for release 4.0

-- com.untangle.tran.httpblocker.HttpBlockerEvent
CREATE TABLE events.tr_httpblk_evt_blk (
    event_id int8 NOT NULL,
    request_id int8,
    action char(1),
    reason char(1),
    category varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));
