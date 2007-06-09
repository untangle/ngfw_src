-- events schema for release-5.0

-----------
-- events |
-----------

-- com.untangle.tran.protofilter.ProtoFilterLogEvent
CREATE TABLE events.n_protofilter_evt (
    event_id int8 NOT NULL,
    pl_endp_id int8,
    protocol text,
    blocked bool,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

----------------
-- constraints |
----------------

-- indeces for reporting

CREATE INDEX tr_protofilter_evt_plepid_idx ON events.tr_protofilter_evt (pl_endp_id);
CREATE INDEX tr_protofilter_evt_ts_idx ON events.tr_protofilter_evt (time_stamp);
