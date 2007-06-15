-- events convert for release 3.1
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

DROP TABLE events.tr_protofilter_tmp;

CREATE TABLE events.tr_protofilter_tmp AS
    SELECT evt.event_id, endp.event_id AS pl_endp_id, protocol::text,
           blocked, evt.time_stamp
    FROM events.tr_protofilter_evt evt JOIN pl_endp endp USING (session_id);

DROP TABLE events.tr_protofilter_evt;
ALTER TABLE events.tr_protofilter_tmp RENAME TO tr_protofilter_evt;
ALTER TABLE events.tr_protofilter_evt ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_protofilter_evt ADD PRIMARY KEY (event_id);

-- indices for reporting

DROP INDEX tr_protofilter_sid_idx;
CREATE INDEX tr_protofilter_evt_plepid_idx ON events.tr_protofilter_evt (pl_endp_id);
