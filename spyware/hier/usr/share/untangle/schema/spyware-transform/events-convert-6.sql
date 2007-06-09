-- convert script for release 3.2

-- com.untangle.tran.spyware.SpywareStatisticEvent
CREATE TABLE events.tr_spyware_statistic_evt (
    event_id int8 NOT NULL,
    pass int4,
    cookie int4,
    activeX int4,
    url int4,
    subnet_access int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));
