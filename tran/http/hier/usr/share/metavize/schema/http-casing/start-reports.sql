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

DROP TABLE reports.maxwebpage;
CREATE TABLE reports.maxwebpage AS
  SELECT COALESCE(MAX(time_stamp), timestamp '2004-01-01') AS time_stamp
    FROM reports.webpages;

DROP TABLE reports.newpages;
CREATE TABLE reports.newpages AS
  SELECT MAX(evt.event_id) AS event_id, MAX(evt.request_id) AS request_id,
         host, COALESCE(MAX(resp.content_length), 0) AS content_length,
         MAX(evt.time_stamp) AS time_stamp
     FROM tr_http_evt_req evt
       LEFT OUTER JOIN tr_http_evt_resp resp
         ON (evt.request_id = resp.request_id AND resp.content_length > 0)
     WHERE evt.time_stamp > (SELECT MAX(time_stamp) FROM reports.maxwebpage)
     GROUP BY evt.event_id, host;

-- DROP TABLE reports.maxwebpage;

INSERT INTO reports.webpages
  SELECT evt.request_id, COALESCE(NULLIF(name, ''),
         HOST(c_client_addr)) AS hname,
         CASE FLOOR(RANDOM() * 10) WHEN 0 THEN 'jdi' WHEN 1 THEN 'jdi' WHEN 2 THEN 'dmorris'
         WHEN 3 THEN 'cng' WHEN 4 THEN 'cng' WHEN 5 THEN 'cng' WHEN 6 THEN 'amread' ELSE null END,
         c_client_addr, c_server_addr, c_server_port, evt.host,
         evt.content_length, evt.time_stamp
     FROM reports.newpages evt
       JOIN tr_http_req_line line USING (request_id)
       JOIN pl_endp endp ON (line.pl_endp_id = endp.event_id)
       LEFT OUTER JOIN merged_address_map mam
         ON (endp.c_client_addr = mam.addr AND evt.time_stamp >= mam.start_time
             AND evt.time_stamp < mam.end_time);

-- DROP TABLE reports.newpages;

CREATE INDEX webpages_ts_idx ON reports.webpages (time_stamp);
