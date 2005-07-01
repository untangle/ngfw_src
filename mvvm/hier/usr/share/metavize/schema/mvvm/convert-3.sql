-- convert script for release 1.5

DROP TABLE pl_endp;
DROP TABLE pl_stats;

CREATE TABLE pl_endp (
    event_id,
    time_stamp,
    session_id,
    proto,
    create_date,
    client_intf,
    server_intf,
    c_client_addr,
    s_client_addr,
    c_server_addr,
    s_server_addr,
    c_client_port,
    s_client_port,
    c_server_port,
    s_server_port)
AS SELECT id, now(), session_id, proto, create_date, client_intf,
          server_intf, c_client_addr, s_client_addr, c_server_addr,
          s_server_addr, c_client_port, s_client_port, c_server_port,
          s_server_port
   FROM pipeline_info;

ALTER TABLE pl_endp ADD CONSTRAINT pl_endp_pkey PRIMARY KEY (event_id);
ALTER TABLE pl_endp ALTER COLUMN event_id SET NOT NULL;

CREATE TABLE pl_stats (
    event_id,
    time_stamp,
    session_id,
    raze_date,
    c2p_bytes,
    s2p_bytes,
    p2c_bytes,
    p2s_bytes,
    c2p_chunks,
    s2p_chunks,
    p2c_chunks,
    p2s_chunks)
AS SELECT nextval('hibernate_sequence'), now(), session_id, raze_date,
          c2p_bytes, s2p_bytes, p2c_bytes, p2s_bytes, c2p_chunks,
          s2p_chunks, p2c_chunks, p2s_chunks
   FROM pipeline_info;

ALTER TABLE pl_stats ADD CONSTRAINT pl_stats_pkey PRIMARY KEY (event_id);
ALTER TABLE pl_stats ALTER COLUMN event_id SET NOT NULL;

DROP TABLE pipeline_info;
DROP TABLE mvvm_evt_pipeline;

-- Table for shield events
CREATE TABLE shield_evt (
        event_id int8 NOT NULL,
        ip inet,
        reputation float8,
        mode int4,
        limited int4,
        rejected int4,
        dropped int4,
        time_stamp timestamp,
        PRIMARY KEY (event_id));

VACUUM ANALYZE;