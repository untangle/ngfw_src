-- convert for release 3.2

DROP TABLE events.tr_ids_tmp;

CREATE TABLE events.tr_ids_tmp (
	event_id int8 NOT NULL,
        pl_endp_id int8,
        rule_sid int4,
	blocked bool,
	classification text,
	message text,
	time_stamp timestamp );

INSERT INTO events.tr_ids_tmp 
  (SELECT event_id, pl_endp_id, rule_sid, blocked,
          'Classification is not available', message::text,
          time_stamp
     FROM events.tr_ids_evt);

DROP TABLE events.tr_ids_evt;
ALTER TABLE events.tr_ids_tmp RENAME TO tr_ids_evt;
ALTER TABLE events.tr_ids_evt ADD PRIMARY KEY (event_id);

CREATE INDEX tr_ids_evt_plepid_idx ON events.tr_ids_evt (pl_endp_id);
