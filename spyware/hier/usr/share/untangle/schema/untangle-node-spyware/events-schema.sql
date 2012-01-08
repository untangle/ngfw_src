-- SpywareAccessEvent
CREATE TABLE events.n_spyware_evt_access (
    event_id int8 NOT NULL,
    session_id int8,
    ipmaddr inet,
    ident text,
    blocked bool,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- SpywareCookieEvent
CREATE TABLE events.n_spyware_evt_cookie (
    event_id int8 NOT NULL,
    request_id int8,
    ident text,
    to_server bool,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- SpywareUrlEvent
CREATE TABLE events.n_spyware_evt_url (
    event_id int8 NOT NULL,
    request_id int8,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE INDEX n_spyware_evt_cookie_request_id_idx ON events.n_spyware_evt_cookie (request_id);
CREATE INDEX n_spyware_evt_url_request_id_idx ON events.n_spyware_evt_url (request_id);
CREATE INDEX n_spyware_evt_access_session_id_idx ON events.n_spyware_evt_access (session_id);
CREATE INDEX n_spyware_evt_cookie_time_stamp_idx ON events.n_spyware_evt_cookie (time_stamp);
CREATE INDEX n_spyware_evt_url_time_stamp_idx ON events.n_spyware_evt_url (time_stamp);
CREATE INDEX n_spyware_evt_activex_time_stamp_idx ON events.n_spyware_evt_activex (time_stamp);
CREATE INDEX n_spyware_evt_access_time_stamp_idx ON events.n_spyware_evt_access (time_stamp);
