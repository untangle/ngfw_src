
create table events.n_ips_evt (
	event_id int8 NOT NULL,
        session_id int8,
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

CREATE INDEX n_ips_evt_session_id_idx ON events.n_ips_evt (session_id);
CREATE INDEX n_ips_evt_time_stamp_idx ON events.n_ips_evt (time_stamp);
