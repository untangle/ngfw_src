-- convert for release 3.1

--------------------
-- link to pl_endp |
--------------------

DROP TABLE events.tr_ids_tmp;

CREATE TABLE events.tr_ids_tmp AS
  SELECT evt.event_id, endp.event_id AS pl_endp_id, evt.message::text,
         evt.blocked, -1 AS rule_sid, evt.time_stamp
  FROM events.tr_ids_evt evt JOIN pl_endp endp USING (session_id);

DROP INDEX tr_ids_evt_sid_idx;
DROP TABLE events.tr_ids_evt;

ALTER TABLE events.tr_ids_tmp RENAME TO tr_ids_evt;
ALTER TABLE events.tr_ids_evt ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_ids_evt ADD PRIMARY KEY (event_id);

CREATE INDEX tr_ids_evt_plepid_idx ON events.tr_ids_evt (pl_endp_id);

DROP TABLE events.tr_ids_statistic_tmp;

CREATE TABLE events.tr_ids_statistic_tmp AS
  SELECT event_id, ids_scanned AS dnc, ids_passed AS logged,
         ids_blocked AS blocked, time_stamp
  FROM events.tr_ids_statistic_evt;

DROP TABLE events.tr_ids_statistic_evt;

ALTER TABLE events.tr_ids_statistic_tmp RENAME TO tr_ids_statistic_evt;
ALTER TABLE events.tr_ids_statistic_evt ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_ids_statistic_evt ADD PRIMARY KEY (event_id);
