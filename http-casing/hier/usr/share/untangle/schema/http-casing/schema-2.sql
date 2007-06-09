-- schema for release 1.4b

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
