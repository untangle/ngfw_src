-- convert script for release 1.5

-- create HttpSettings.

CREATE TABLE tr_http_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    enabled bool NOT NULL,
    PRIMARY KEY (settings_id));

INSERT INTO tr_http_settings
    (SELECT nextval('hibernate_sequence'), tid, TRUE
     FROM transform_persistent_state WHERE name = 'http-casing');

ALTER TABLE tr_http_settings ADD CONSTRAINT fkf4229df91446f FOREIGN KEY (tid) REFERENCES tid;

-- indexes for reporting

CREATE INDEX tr_http_evt_req_ts_idx ON tr_http_evt_req (time_stamp);
CREATE INDEX tr_http_evt_resp_rid_idx ON tr_http_evt_resp (request_id);
