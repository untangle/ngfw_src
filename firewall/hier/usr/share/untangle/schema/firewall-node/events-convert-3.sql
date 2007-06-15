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

----------------------
-- PipelineEndpoints |
----------------------

DROP TABLE events.tr_firewall_tmp;

CREATE TABLE events.tr_firewall_tmp AS
    SELECT evt.event_id, endp.event_id AS pl_endp_id, was_blocked,
          rule_id, rule_index, evt.time_stamp
    FROM events.tr_firewall_evt evt JOIN events.pl_endp endp USING (session_id);

DROP TABLE events.tr_firewall_evt;
ALTER TABLE events.tr_firewall_tmp RENAME TO tr_firewall_evt;
ALTER TABLE events.tr_firewall_evt ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_firewall_evt ADD PRIMARY KEY (event_id);

DROP INDEX tr_firewall_evt_sid_idx;
CREATE INDEX tr_firewall_evt_plepid_idx ON events.tr_firewall_evt (pl_endp_id);
