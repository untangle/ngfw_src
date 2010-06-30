-- events convert for release-3.1
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

---------------------
-- point at pl_endp |
---------------------

DROP TABLE events.mvvm_tmp;

CREATE TABLE events.mvvm_tmp AS
    SELECT evt.event_id, CAST (evt.time_stamp AS timestamp without time zone), endp.event_id AS pl_endp_id,
           raze_date, c2p_bytes, s2p_bytes, p2c_bytes, p2s_bytes,
           c2p_chunks, s2p_chunks, p2c_chunks, p2s_chunks
    FROM events.pl_stats evt JOIN events.pl_endp endp USING (session_id);

DROP TABLE events.pl_stats;
ALTER TABLE events.mvvm_tmp RENAME TO pl_stats;
ALTER TABLE events.pl_stats ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.pl_stats ADD PRIMARY KEY (event_id);

-- com.untangle.mvvm.engine.LoginEvent
DROP TABLE events.mvvm_tmp;

CREATE TABLE events.mvvm_tmp AS
    SELECT event_id, client_addr, login::text, local, succeeded, reason,
           time_stamp
    FROM events.mvvm_login_evt;

DROP TABLE events.mvvm_login_evt;
ALTER TABLE events.mvvm_tmp RENAME TO mvvm_login_evt;
ALTER TABLE events.mvvm_login_evt ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.mvvm_login_evt ADD PRIMARY KEY (event_id);

-------------------------
-- recreate constraints |
-------------------------

-- indexes

DROP INDEX pl_stats_sid_idx;
CREATE INDEX pl_stats_plepid_idx ON events.pl_stats (pl_endp_id);

-----------------
-- remove cruft |
-----------------

DROP TABLE old_pl_endp;
