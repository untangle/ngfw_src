-- $HeadURL: svn://chef/work/src/http-casing/hier/usr/share/untangle/schema/untangle-casing-http/events-convert-7.sql $
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

-- events convert for release-5.2

DROP TABLE events.new_n_http_req_line;

CREATE TABLE events.new_n_http_req_line (
    request_id int8 NOT NULL,
    pl_endp_id int8,
    method char(1),
    uri text,
    time_stamp timestamp);

INSERT INTO events.new_n_http_req_line
  SELECT line.request_id, line.pl_endp_id, line.method, line.uri, reqevt.time_stamp FROM n_http_req_line line
  INNER JOIN n_http_evt_req reqevt ON (reqevt.request_id = line.request_id);

DROP TABLE events.n_http_req_line;
ALTER TABLE events.new_n_http_req_line RENAME TO n_http_req_line;
ALTER TABLE events.n_http_req_line ADD PRIMARY KEY (request_id);
