-- events schema for release-5.0
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

-- com.untangle.tran.nat.DhcpLeaseEvent
CREATE TABLE events.n_router_evt_dhcp (
    event_id int8 NOT NULL,
    mac varchar(255),
    hostname varchar(255),
    ip inet,
    end_of_lease timestamp,
    event_type int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.nat.DhcpAbsoluteLease
CREATE TABLE events.n_router_dhcp_abs_lease (
    event_id int8 NOT NULL,
    mac varchar(255),
    hostname varchar(255),
    ip inet, end_of_lease timestamp,
    event_type int4,
    PRIMARY KEY (event_id));

-- com.untangle.tran.nat.DhcpAbsoluteEvent
CREATE TABLE events.n_router_evt_dhcp_abs (
    event_id int8 NOT NULL,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.nat.DhcpAbsoluteEvent.absoluteLeaseList
CREATE TABLE events.n_router_evt_dhcp_abs_leases (
    event_id int8 NOT NULL,
    lease_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (event_id, position));



