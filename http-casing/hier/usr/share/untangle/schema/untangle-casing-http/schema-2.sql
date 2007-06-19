-- schema for release 1.4b
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
