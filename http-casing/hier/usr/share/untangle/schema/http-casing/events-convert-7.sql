-- events convert for release-4.2
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
