-- schema for release-1.5

CREATE TABLE tr_http_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    enabled bool NOT NULL,
    PRIMARY KEY (settings_id));

CREATE TABLE tr_http_evt_resp (
    event_id int8 NOT NULL,
    request_id int8,
    content_type varchar(255),
    content_length int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE tr_http_evt_req (
    event_id int8 NOT NULL,
    session_id int4,
    request_id int8,
    host varchar(255),
    content_length int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE tr_http_req_line (
    request_id int8 NOT NULL,
    method char(1),
    uri varchar(255),
    http_version varchar(10),
    PRIMARY KEY (request_id));

ALTER TABLE tr_http_settings ADD CONSTRAINT fkf4229df91446f FOREIGN KEY (tid) REFERENCES tid;

ALTER TABLE tr_http_evt_resp ADD CONSTRAINT FKC9BB12A21F20A4EB FOREIGN KEY (request_id) REFERENCES tr_http_req_line ON DELETE CASCADE;

ALTER TABLE tr_http_evt_req ADD CONSTRAINT FK40505B6C1F20A4EB FOREIGN KEY (request_id) REFERENCES tr_http_req_line ON DELETE CASCADE;

CREATE INDEX tr_http_evt_req_tstmp ON tr_http_evt_req (time_stamp);
CREATE INDEX tr_http_evt_resp_reqid ON tr_http_evt_resp (request_id);
