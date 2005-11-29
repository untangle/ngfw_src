-- events convert for release 3.1

DROP TABLE events.tr_protofilter_tmp;

CREATE TABLE events.tr_protofilter_tmp AS
    SELECT evt.event_id, endp.event_id AS pl_endp_id, protocol::text,
           blocked, evt.time_stamp
    FROM events.tr_protofilter_evt evt JOIN pl_endp endp USING (session_id);

DROP TABLE events.tr_protofilter_evt;
ALTER TABLE events.tr_protofilter_tmp RENAME TO tr_protofilter_evt;
ALTER TABLE events.tr_protofilter_evt ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_protofilter_evt ADD PRIMARY KEY (event_id);

-- indices for reporting

DROP INDEX tr_protofilter_sid_idx;
CREATE INDEX tr_protofilter_evt_plepid_idx ON events.tr_protofilter_evt (pl_endp_id);
