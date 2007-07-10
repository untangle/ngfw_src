-- events conversion for release-5.0
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

ALTER TABLE events.tr_http_evt_resp RENAME TO n_http_evt_resp;
ALTER TABLE events.tr_http_evt_req RENAME TO n_http_evt_req;
ALTER TABLE events.tr_http_req_line RENAME TO n_http_req_line;

ALTER TABLE events.tr_http_evt_req_ts_idx RENAME TO n_http_evt_req_ts_idx;
ALTER TABLE events.tr_http_evt_req_rid_idx RENAME TO n_http_evt_req_rid_idx;
ALTER TABLE events.tr_http_evt_resp_rid_idx RENAME TO n_http_evt_resp_rid_idx;
