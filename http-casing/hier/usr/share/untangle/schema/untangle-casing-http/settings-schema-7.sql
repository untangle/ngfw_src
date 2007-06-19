-- settings schema for release-4.2
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

-- com.untangle.tran.http.HttpSettings
CREATE TABLE settings.tr_http_settings (
    settings_id int8 NOT NULL,
    enabled bool NOT NULL,
    non_http_blocked bool NOT NULL,
    max_header_length int4 NOT NULL,
    block_long_headers bool NOT NULL,
    max_uri_length int4 NOT NULL,
    block_long_uris bool NOT NULL,
    PRIMARY KEY (settings_id));
