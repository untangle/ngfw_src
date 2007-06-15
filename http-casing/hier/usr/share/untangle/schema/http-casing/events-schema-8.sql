-- events schema for release-5.0
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

-----------
-- tables |
-----------

-- com.untangle.tran.http.HttpResponseEvent
CREATE TABLE events.n_http_evt_resp (
    event_id int8 NOT NULL,
    request_id int8,
    content_type text,
    content_length int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.http.HttpRequestEvent
CREATE TABLE events.n_http_evt_req (
    event_id int8 NOT NULL,
    request_id int8,
    host text,
    content_length int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.http.RequestLine
CREATE TABLE events.n_http_req_line (
    request_id int8 NOT NULL,
    pl_endp_id int8,
    method char(1),
    uri text,
    time_stamp timestamp,
    PRIMARY KEY (request_id));

----------------
-- constraints |
----------------

-- indices for reporting

CREATE INDEX n_http_evt_req_ts_idx ON events.n_http_evt_req (time_stamp);
CREATE INDEX n_http_evt_req_rid_idx ON events.n_http_evt_req (request_id);
CREATE INDEX n_http_evt_resp_rid_idx ON events.n_http_evt_resp (request_id);
-- No resp or line time_stamp index since no event log is filled from them.
