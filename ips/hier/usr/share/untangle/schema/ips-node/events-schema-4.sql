-- events schema for release-5.0

-----------
-- events |
-----------
create table events.n_ips_evt (
	event_id int8 NOT NULL,
        pl_endp_id int8,
	classification text,
	message text,
	blocked bool,
        rule_sid int4,
	time_stamp timestamp,
	PRIMARY KEY (event_id));

create table events.n_ips_statistic_evt (
	event_id int8 NOT NULL,
	dnc int4,
	logged int4,
	blocked int4,
	time_stamp timestamp,
	PRIMARY KEY (event_id));

-- indices for reporting
CREATE INDEX n_ips_evt_plepid_idx ON events.n_ips_evt (pl_endp_id);
CREATE INDEX n_ips_evt_ts_idx ON events.n_ips_evt (time_stamp);
