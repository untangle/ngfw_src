ALTER TABLE reports.sessions ADD COLUMN event_id text;
UPDATE reports.sessions SET event_id = pl_endp_id::text;

ALTER TABLE reports.n_http_events ADD COLUMN event_id text;
UPDATE reports.n_http_events SET event_id = session_id::ext;

ALTER TABLE reports.n_mail_addrs ADD COLUMN sender text;
ALTER TABLE reports.n_mail_addrs ADD COLUMN vendor text;
ALTER TABLE reports.n_mail_addrs ADD COLUMN event_id text;
UPDATE reports.n_mail_addrs SET event_id = md5(session_id::text || addr::text || addr_kind || msg_id::text);

ALTER TABLE reports.n_openvpn_stats ADD COLUMN event_id serial;

ALTER TABLE reports.n_cpd_login_events ADD COLUMN event_id serial;
ALTER TABLE reports.n_cpd_block_events ADD COLUMN event_id serial;
