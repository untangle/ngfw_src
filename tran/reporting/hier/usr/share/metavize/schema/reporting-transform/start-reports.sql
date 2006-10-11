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
        s2p_bytes int8,
        p2s_bytes int8,
        time_stamp timestamp,
        PRIMARY KEY (pl_endp_id));

INSERT INTO reports.sessions
  SELECT endp.event_id, COALESCE(NULLIF(name, ''), HOST(c_client_addr)) AS hname,
         case floor(random() * 10) when 0 then 'jdi' when 1 then 'jdi' when 2 then 'dmorris'
         when 3 then 'cng' when 4 then 'cng' when 5 then 'cng' when 6 then 'amread' else null end,
         c_client_addr, c_server_addr, c_server_port, s2p_bytes, p2s_bytes, endp.create_date
    FROM pl_endp endp
    JOIN pl_stats stats ON endp.event_id = stats.pl_endp_id
    LEFT OUTER JOIN merged_address_map mam ON (endp.c_client_addr = mam.addr AND endp.create_date >= mam.start_time AND endp.create_date < mam.end_time);

CREATE INDEX sessions_ts_idx ON reports.sessions (time_stamp);
