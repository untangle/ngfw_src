-- reports start for release-4.1

CREATE SCHEMA reports;

DROP TABLE reports.webpages;

CREATE TABLE reports.webpages (
        request_id int8 NOT NULL,
        hname text,
        uid text,
        c_client_addr inet,
        c_server_addr inet,
        c_server_port int4,
        host text,
        content_length int4,
        time_stamp timestamp,
        PRIMARY KEY (request_id));
        
INSERT INTO reports.webpages
  SELECT evt.request_id, COALESCE(NULLIF(name, ''), HOST(c_client_addr)) AS hname,
         case floor(random() * 10) when 0 then 'jdi' when 1 then 'jdi' when 2 then 'dmorris'
         when 3 then 'cng' when 4 then 'cng' when 5 then 'cng' when 6 then 'amread' else null end,
         c_client_addr, c_server_addr, c_server_port, host, COALESCE(resp.content_length, 0) AS content_length, evt.time_stamp
    FROM tr_http_evt_req evt
      JOIN tr_http_req_line line USING (request_id)
      JOIN pl_endp endp ON (line.pl_endp_id = endp.event_id)
      LEFT OUTER JOIN tr_http_evt_resp resp ON (evt.request_id = resp.request_id AND resp.content_length > 0)
      LEFT OUTER JOIN merged_address_map mam ON (endp.c_client_addr = mam.addr AND evt.time_stamp >= mam.start_time AND evt.time_stamp < mam.end_time);

CREATE INDEX webpages_ts_idx ON reports.webpages (time_stamp);
