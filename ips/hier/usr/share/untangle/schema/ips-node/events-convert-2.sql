-- convert for release 3.2
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

DROP TABLE events.tr_ids_tmp;

CREATE TABLE events.tr_ids_tmp (
	event_id int8 NOT NULL,
        pl_endp_id int8,
        rule_sid int4,
	blocked bool,
	classification text,
	message text,
	time_stamp timestamp );

INSERT INTO events.tr_ids_tmp 
  (SELECT event_id, pl_endp_id, rule_sid, blocked,
          'Classification is not available', message::text,
          time_stamp
     FROM events.tr_ids_evt);

DROP TABLE events.tr_ids_evt;
ALTER TABLE events.tr_ids_tmp RENAME TO tr_ids_evt;
ALTER TABLE events.tr_ids_evt ADD PRIMARY KEY (event_id);

CREATE INDEX tr_ids_evt_plepid_idx ON events.tr_ids_evt (pl_endp_id);
