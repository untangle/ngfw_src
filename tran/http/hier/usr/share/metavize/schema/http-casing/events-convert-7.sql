-- events convert for release-4.2

CREATE TABLE events.new_tr_http_req_line (
    request_id int8 NOT NULL,
    pl_endp_id int8,
    method char(1),
    uri text,
    time_stamp timestamp,
    PRIMARY KEY (request_id));

INSERT INTO events.new_tr_http_req_line
  SELECT line.*, reqevt.time_stamp FROM tr_http_req_line line
  INNER JOIN tr_http_evt_req reqevt ON (reqevt.request_id = line.request_id);

DROP TABLE events.tr_http_req_line;
ALTER TABLE events.new_tr_http_req_line RENAME TO tr_http_req_line;
