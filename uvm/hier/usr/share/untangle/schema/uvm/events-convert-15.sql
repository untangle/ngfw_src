-- 9.2

ALTER TABLE events.pl_stats DROP COLUMN session_id;
ALTER TABLE events.pl_stats DROP COLUMN proto;
ALTER TABLE events.pl_stats DROP COLUMN client_intf;
ALTER TABLE events.pl_stats DROP COLUMN server_intf;
ALTER TABLE events.pl_stats DROP COLUMN c_client_addr;
ALTER TABLE events.pl_stats DROP COLUMN s_client_addr;
ALTER TABLE events.pl_stats DROP COLUMN c_server_addr;
ALTER TABLE events.pl_stats DROP COLUMN s_server_addr;
ALTER TABLE events.pl_stats DROP COLUMN c_client_port;
ALTER TABLE events.pl_stats DROP COLUMN s_client_port;
ALTER TABLE events.pl_stats DROP COLUMN c_server_port;
ALTER TABLE events.pl_stats DROP COLUMN s_server_port;
ALTER TABLE events.pl_stats DROP COLUMN policy_id;
ALTER TABLE events.pl_stats DROP COLUMN policy_inbound;

ALTER TABLE events.pl_stats DROP COLUMN session_id;
ALTER TABLE events.pl_stats RENAME COLUMN pl_endp_id to session_id;

CREATE INDEX pl_stats_session_id_idx ON events.pl_stats (session_id);

ALTER TABLE events.pl_endp ALTER COLUMN session_id TYPE int8;
ALTER TABLE events.pl_endp DROP COLUMN policy_inbound;
ALTER TABLE events.pl_endp ADD COLUMN username text;



