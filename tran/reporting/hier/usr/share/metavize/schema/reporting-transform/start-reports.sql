-- reports start for release-4.1

CREATE SCHEMA reports;

DROP TABLE reports.sessions;

CREATE TABLE reports.sessions (
        pl_endp_id int8 NOT NULL,
        hname text,
        uid text,
        c_client_addr inet,
        c_server_addr inet,
        c_server_port int4,
        client_intf int2,
        c2p_bytes int8,
        p2c_bytes int8,
        s2p_bytes int8,
        p2s_bytes int8,
        time_stamp timestamp,
        PRIMARY KEY (pl_endp_id));

INSERT INTO reports.sessions
  SELECT endp.event_id,
         COALESCE(NULLIF(mam.name, ''), HOST(endp.c_client_addr)) AS hname,
         CASE FLOOR(RANDOM() * 10) WHEN 0 THEN 'jdi' WHEN 1 THEN 'jdi' WHEN 2 THEN 'dmorris'
         WHEN 3 THEN 'cng' WHEN 4 THEN 'cng' WHEN 5 THEN 'cng' WHEN 6 THEN 'amread' ELSE null END,
         endp.c_client_addr, endp.c_server_addr, endp.c_server_port,
         endp.client_intf,
         stats.c2p_bytes, stats.p2c_bytes, stats.s2p_bytes, stats.p2s_bytes,
         endp.create_date
    FROM pl_endp endp
    JOIN pl_stats stats ON (endp.event_id = stats.pl_endp_id)
    LEFT OUTER JOIN merged_address_map mam
      ON (endp.c_client_addr = mam.addr AND endp.create_date >= mam.start_time
          AND endp.create_date < mam.end_time);

CREATE INDEX sessions_ts_idx ON reports.sessions (time_stamp);
