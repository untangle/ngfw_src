-- events schema for release-5.0
-- $HeadURL: svn://chef/work/src/firewall/hier/usr/share/untangle/schema/untangle-node-firewall/events-schema.sql $
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

-- com.untangle.node.cpd.BlockEvent
-- CREATE TABLE events.n_cpd_block_evt (
--     event_id INT8 NOT NULL,
--     time_stamp TIMESTAMP,
--     proto INT2,
--     client_intf INT2,
--     client_address INET,
--     client_port INT4,
--     server_address INET,
--     server_port INT4,
--     PRIMARY KEY (event_id));

-- CPDLoginEvent
-- CREATE TABLE events.n_cpd_login_evt (
--     event_id    INT8 NOT NULL,
--     login_name  TEXT,
--     event	TEXT,
--     auth_type   TEXT,
--     time_stamp  TIMESTAMP,
--     client_addr inet,
--     PRIMARY KEY (event_id));

-- com.untangle.node.cpd.HostDatabaseEntry
CREATE TABLE events.n_cpd_host_database_entry (
    entry_id        INT8 NOT NULL,
    hw_addr         TEXT,
    ipv4_addr       INET,
    username        TEXT,
    last_session    TIMESTAMP,
    session_start   TIMESTAMP,
    expiration_date TIMESTAMP,
    PRIMARY KEY     (entry_id));

----------------
-- constraints |
----------------

-- indices for reporting
--CREATE INDEX n_cpd_block_evt_ts_idx ON events.n_cpd_block_evt (time_stamp);
--CREATE INDEX n_cpd_login_evt_ts_idx ON events.n_cpd_login_evt (time_stamp);

CREATE INDEX n_cpd_host_database_last_session_idx ON 
       events.n_cpd_host_database_entry (last_session);
       
-- For querying on sessions that are expired
CREATE INDEX n_cpd_host_database_expiration_date_idx ON
       events.n_cpd_host_database_entry (expiration_date);

CREATE INDEX n_cpd_host_database_username_idx ON
       events.n_cpd_host_database_entry (username);

CREATE INDEX n_cpd_host_database_ipv4_addr_idx ON
       events.n_cpd_host_database_entry (ipv4_addr);



