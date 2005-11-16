-- convert for release 3.1

--------------------
-- link to pl_endp |
--------------------

CREATE TABLE events.tr_ids_tmp AS
    SELECT evt.event_id, endp.event_id AS pl_endp_id, evt.message::text,
           evt.blocked, evt.time_stamp
    FROM events.tr_ids_evt evt JOIN pl_endp endp USING (session_id);

DROP INDEX tr_ids_evt_sid_idx;
DROP TABLE events.tr_ids_evt;

ALTER TABLE events.tr_ids_tmp RENAME TO tr_ids_evt;
ALTER TABLE events.tr_ids_evt ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_ids_evt ADD PRIMARY KEY (event_id);

CREATE INDEX tr_ids_evt_sid_idx ON events.tr_ids_evt (event_id);
