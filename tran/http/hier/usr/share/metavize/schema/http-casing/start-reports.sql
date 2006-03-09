-- reports start for release-3.2

CREATE SCHEMA reports;

DROP TABLE reports.webpages;

CREATE TABLE reports.webpages AS
  SELECT evt.request_id, COALESCE(NULLIF(name, ''), HOST(c_client_addr)) AS hname, c_client_addr, c_server_addr, c_server_port, host, COALESCE(resp.content_length, 0) AS content_length, evt.time_stamp
    FROM tr_http_evt_req evt
      JOIN tr_http_req_line line USING (request_id)
      LEFT OUTER JOIN tr_http_evt_resp resp USING (request_id)
      JOIN pl_endp endp ON (line.pl_endp_id = endp.event_id)
      LEFT OUTER JOIN merged_address_map mam ON (endp.c_client_addr = mam.addr AND evt.time_stamp >= mam.start_time AND evt.time_stamp < mam.end_time);

CREATE INDEX webpages_rid_idx ON reports.webpages (request_id);
CREATE INDEX webpages_hname_idx ON reports.webpages (hname);
CREATE INDEX webpages_ts_idx ON reports.webpages (time_stamp);
