-- events conversion for release-5.0

ALTER TABLE events.tr_http_evt_resp RENAME TO n_http_evt_resp;
ALTER TABLE events.tr_http_evt_req RENAME TO n_http_evt_req;
ALTER TABLE events.tr_http_req_line RENAME TO n_http_req_line;
