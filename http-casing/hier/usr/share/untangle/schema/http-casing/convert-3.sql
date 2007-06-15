-- convert script for release 2.5
-- $HeadURL$
-- Copyright (c) 2003-2007 Untangle, Inc. 
--
-- This program is free software; you can redistribute it and/or modify
-- it under the terms of the GNU General Public License, version 2,
-- as published by the Free Software Foundation.
--
-- This program is distributed in the hope that it will be useful, but
-- AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
-- NONINFRINGEMENT.  See the GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with this program; if not, write to the Free Software
-- Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
--

-----------------------------------
-- move old tables to new schemas |
-----------------------------------

-- com.untangle.tran.http.HttpResponseEvent
CREATE TABLE events.tr_http_evt_resp AS SELECT * FROM public.tr_http_evt_resp;

ALTER TABLE events.tr_http_evt_resp
    ADD CONSTRAINT tr_http_evt_resp_pkey PRIMARY KEY (event_id);
ALTER TABLE events.tr_http_evt_resp
    ALTER COLUMN event_id SET NOT NULL;

-- com.untangle.tran.http.HttpRequestEvent
CREATE TABLE events.tr_http_evt_req AS SELECT * FROM public.tr_http_evt_req;

ALTER TABLE events.tr_http_evt_req
    ADD CONSTRAINT tr_http_evt_req_pkey PRIMARY KEY (event_id);
ALTER TABLE events.tr_http_evt_req
    ALTER COLUMN event_id SET NOT NULL;

-- com.untangle.tran.http.RequestLine
CREATE TABLE events.tr_http_req_line AS SELECT * FROM public.tr_http_req_line;

ALTER TABLE events.tr_http_req_line
    ADD CONSTRAINT tr_http_req_line_pkey PRIMARY KEY (request_id);
ALTER TABLE events.tr_http_req_line
    ALTER COLUMN request_id SET NOT NULL;

--------------------
-- drop old tables |
--------------------

DROP TABLE public.tr_http_evt_resp;
DROP TABLE public.tr_http_evt_req;
DROP TABLE public.tr_http_req_line;

---------------
-- new tables |
---------------

-- com.untangle.tran.http.HttpSettings
CREATE TABLE settings.tr_http_settings (
    settings_id,
    tid,
    enabled,
    non_http_blocked,
    max_header_length,
    block_long_headers,
    max_uri_length,
    block_long_uris)
AS SELECT nextval('hibernate_sequence'), tid, true, false,
          4096::int4, false, 4096::int4, false
   FROM transform_persistent_state WHERE name = 'http-casing';

ALTER TABLE settings.tr_http_settings
    ADD CONSTRAINT tr_http_settings_pkey PRIMARY KEY (settings_id);
ALTER TABLE settings.tr_http_settings
    ADD CONSTRAINT tr_http_settings_uk UNIQUE (tid);
ALTER TABLE settings.tr_http_settings
    ALTER COLUMN tid SET NOT NULL;
ALTER TABLE settings.tr_http_settings
    ALTER COLUMN enabled SET NOT NULL;
ALTER TABLE settings.tr_http_settings
    ALTER COLUMN non_http_blocked SET NOT NULL;
ALTER TABLE settings.tr_http_settings
    ALTER COLUMN max_header_length SET NOT NULL;
ALTER TABLE settings.tr_http_settings
    ALTER COLUMN block_long_headers SET NOT NULL;
ALTER TABLE settings.tr_http_settings
    ALTER COLUMN max_uri_length SET NOT NULL;
ALTER TABLE settings.tr_http_settings
    ALTER COLUMN block_long_uris SET NOT NULL;

-- foreign key constraints

ALTER TABLE tr_http_settings
    ADD CONSTRAINT fk_tr_http_settings
    FOREIGN KEY (tid) REFERENCES tid;

-- indeces for reporting

CREATE INDEX tr_http_evt_req_ts_idx ON tr_http_evt_req (time_stamp);
CREATE INDEX tr_http_evt_req_sid_idx ON tr_http_evt_req (session_id);
CREATE INDEX tr_http_evt_resp_rid_idx ON tr_http_evt_resp (request_id);

------------
-- analyze |
------------

ANALYZE events.tr_http_evt_resp;
ANALYZE events.tr_http_evt_req;
ANALYZE events.tr_http_req_line;
