-- events schema for release-5.0

-- com.untangle.tran.clamphish.PhishHttpEvent
CREATE TABLE events.n_phish_http_evt (
    event_id int8 NOT NULL,
    request_id int8,
    action char(1),
    category varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE INDEX n_phish_http_evt_ts_idx ON events.n_phish_http_evt (time_stamp);
