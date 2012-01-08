-- ProtoFilterLogEvent
CREATE TABLE events.n_protofilter_evt (
    event_id int8 NOT NULL,
    session_id int8,
    protocol text,
    blocked bool,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE INDEX n_protofilter_evt_session_id_idx ON events.n_protofilter_evt (session_id);
CREATE INDEX n_protofilter_evt_ts_idx ON events.n_protofilter_evt (time_stamp);
