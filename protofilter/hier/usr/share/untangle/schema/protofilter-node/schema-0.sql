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

CREATE TABLE tr_protofilter_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    buffersize int4,
    bytelimit int4,
    chunklimit int4,
    unknownstring varchar(255),
    stripzeros bool,
    PRIMARY KEY (settings_id));

CREATE TABLE tr_protofilter_pattern (
    rule_id int8 NOT NULL,
    protocol varchar(255),
    description varchar(255),
    category varchar(255),
    definition varchar(4096),
    quality varchar(255),
    blocked bool,
    alert bool,
    log bool,
    settings_id int8,
    position int4,
    PRIMARY KEY (rule_id));

CREATE TABLE tr_protofilter_evt (
    event_id int8 NOT NULL,
    session_id int4,
    protocol varchar(255),
    blocked bool,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

ALTER TABLE tr_protofilter_settings ADD CONSTRAINT FK55F095631446F FOREIGN KEY (tid) REFERENCES tid;

ALTER TABLE tr_protofilter_pattern ADD CONSTRAINT FKE929349B79192AB7 FOREIGN KEY (settings_id) REFERENCES tr_protofilter_settings;
