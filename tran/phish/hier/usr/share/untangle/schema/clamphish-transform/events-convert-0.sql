-- events schema for release 4.2

-- com.untangle.tran.clamphish.PhishHttpEvent
CREATE TABLE events.tr_phishhttp_evt (
    event_id int8 NOT NULL,
    request_id int8,
    action char(1),
    category varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));
