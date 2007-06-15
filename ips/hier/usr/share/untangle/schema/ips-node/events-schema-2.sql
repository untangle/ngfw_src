-- schema for release 3.2
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
-- events |
-----------
create table events.TR_IDS_EVT (
	event_id int8 NOT NULL,
        pl_endp_id int8,
	classification text,
	message text,
	blocked bool,
        rule_sid int4,
	time_stamp timestamp,
	PRIMARY KEY (event_id));

create table events.TR_IDS_STATISTIC_EVT (
	event_id int8 NOT NULL,
	dnc int4,
	logged int4,
	blocked int4,
	time_stamp timestamp,
	PRIMARY KEY (event_id));

-- indices for reporting
CREATE INDEX tr_ids_evt_plepid_idx ON events.tr_ids_evt (pl_endp_id);
