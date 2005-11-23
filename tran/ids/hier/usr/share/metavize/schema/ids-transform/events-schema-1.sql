-- schema for release 3.1

-----------
-- events |
-----------
create table events.TR_IDS_EVT (
	event_id int8 NOT NULL,
        pl_endp_id int8,
	message text,
	blocked bool,
        rule_sid int4,
	time_stamp timestamp,
	PRIMARY KEY (event_id));

create table events.TR_IDS_STATISTIC_EVT (
	event_id int8 NOT NULL,
	dnc int4,
	logged int4,
	blocked int4,
	time_stamp timestamp,
	PRIMARY KEY (event_id));

-- indices for reporting

CREATE INDEX tr_ids_evt_plepid_idx ON events.tr_ids_evt (pl_endp_id);
