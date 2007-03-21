-- reports start for release-4.1

--------------------------------------------------------------------------------
-- Create tables as necessary, ignore errors if already present.
SET search_path TO reports,events,public;

CREATE TABLE sessions (
        pl_endp_id int8 NOT NULL,
        time_stamp timestamp NOT NULL,
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
        PRIMARY KEY (pl_endp_id));

-- Just in case
DELETE FROM ONLY reports.sessions;

--------------------------------------------------------------------------------
-- Do the day
DROP TABLE sessions_:dayname;
CREATE TABLE sessions_:dayname (
    CHECK (time_stamp >= TIMESTAMP :daybegin AND time_stamp < TIMESTAMP :dayend)
) INHERITS (sessions);

DROP TABLE newsessions;
CREATE TABLE newsessions AS
  SELECT endp.event_id, endp.time_stamp, mam.name,
         endp.c_client_addr, endp.c_server_addr, endp.c_server_port, endp.client_intf
    FROM pl_endp endp
    LEFT OUTER JOIN merged_address_map mam
      ON (endp.c_client_addr = mam.addr AND endp.time_stamp >= mam.start_time
          AND endp.time_stamp < mam.end_time)
   WHERE endp.time_stamp >= TIMESTAMP :daybegin AND endp.time_stamp <= TIMESTAMP :dayend;

-- Note -- do *not* analyze here or the following query runs 4 times slower!!!

INSERT INTO sessions_:dayname
  SELECT ses.event_id, ses.time_stamp,
         COALESCE(NULLIF(ses.name, ''), HOST(ses.c_client_addr)) AS hname,
         stats.uid, c_client_addr, c_server_addr, c_server_port, client_intf,
         stats.c2p_bytes, stats.p2c_bytes, stats.s2p_bytes, stats.p2s_bytes
    FROM newsessions ses
    JOIN pl_stats stats ON (ses.event_id = stats.pl_endp_id);

DROP TABLE newsessions;

--CREATE INDEX sessions_reqid_idx_:dayname ON sessions_:dayname (request_id);
CREATE INDEX sessions_ts_idx_:dayname ON sessions_:dayname (time_stamp);
CREATE INDEX sessions_hname_idx_:dayname ON sessions_:dayname (hname);
CREATE INDEX sessions_uid_idx_:dayname ON sessions_:dayname (uid);
