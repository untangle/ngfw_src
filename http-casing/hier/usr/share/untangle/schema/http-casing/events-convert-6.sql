-- events convert for release-3.1
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

--------------------
-- remove varchars |
--------------------

-- tr_http_req_line
DROP TABLE events.tr_http_tmp;

CREATE TABLE events.tr_http_tmp AS
    SELECT request_id, endp.event_id AS pl_endp_id, method, uri::text
    FROM events.tr_http_req_line rl
      JOIN events.tr_http_evt_req req USING (request_id)
      JOIN pl_endp endp USING (session_id);

DROP TABLE events.tr_http_req_line;
ALTER TABLE events.tr_http_tmp RENAME TO tr_http_req_line;
ALTER TABLE events.tr_http_req_line ADD PRIMARY KEY (request_id);
ALTER TABLE events.tr_http_req_line ALTER COLUMN request_id SET NOT NULL;

-- tr_http_evt_req
DROP TABLE events.tr_http_tmp;

CREATE TABLE events.tr_http_tmp AS
    SELECT evt.event_id, request_id, host::text,
           content_length, evt.time_stamp
    FROM events.tr_http_evt_req evt
         JOIN events.tr_http_req_line USING (request_id);

DROP TABLE events.tr_http_evt_req;
ALTER TABLE events.tr_http_tmp RENAME TO tr_http_evt_req;
ALTER TABLE events.tr_http_evt_req ADD PRIMARY KEY (event_id);
ALTER TABLE events.tr_http_evt_req ALTER COLUMN event_id SET NOT NULL;

-- tr_http_evt_resp
DROP TABLE events.tr_http_tmp;

CREATE TABLE events.tr_http_tmp AS
    SELECT event_id, request_id, content_type::text, content_length,
           time_stamp
    FROM events.tr_http_evt_resp
         JOIN events.tr_http_req_line USING (request_id);

DROP TABLE events.tr_http_evt_resp;
ALTER TABLE events.tr_http_tmp RENAME TO tr_http_evt_resp;
ALTER TABLE events.tr_http_evt_resp ADD PRIMARY KEY (event_id);
ALTER TABLE events.tr_http_evt_resp ALTER COLUMN event_id SET NOT NULL;

---------------------
-- recreate indeces |
---------------------

CREATE INDEX tr_http_evt_req_ts_idx ON events.tr_http_evt_req (time_stamp);
CREATE INDEX tr_http_evt_req_rid_idx ON events.tr_http_evt_req (request_id);
CREATE INDEX tr_http_evt_resp_rid_idx ON events.tr_http_evt_resp (request_id);
