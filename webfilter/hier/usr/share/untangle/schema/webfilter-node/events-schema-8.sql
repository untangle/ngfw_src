-- events schema for release-5.0

-- com.untangle.tran.httpblocker.HttpBlockerEvent
CREATE TABLE events.n_webfilter_evt_blk (
    event_id int8 NOT NULL,
    request_id int8,
    action char(1),
    reason char(1),
    category varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));


-- indices for reporting

CREATE INDEX n_webfilter_evt_blk_ts_idx ON events.n_webfilter_evt_blk (time_stamp);
