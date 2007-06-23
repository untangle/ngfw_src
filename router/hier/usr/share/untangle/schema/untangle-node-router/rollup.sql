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

-- Use 3 days to make sure there are no lease events that other logs may need
-- DELETE FROM n_router_evt_dhcp_abs WHERE time_stamp < ((:cutoff)::timestamp - interval '3 days' );
-- DELETE FROM n_router_evt_dhcp     WHERE time_stamp < ((:cutoff)::timestamp - interval '3 days' );

-- Delete all of the old events from statistics
DELETE FROM n_router_statistic_evt WHERE time_stamp < (:cutoff)::timestamp;

ANALYZE n_router_evt_dhcp;
ANALYZE n_router_evt_dhcp_abs;

DELETE FROM n_router_evt_dhcp_abs_leases
    WHERE NOT EXISTS
        (SELECT 1 FROM n_router_evt_dhcp_abs
            WHERE n_router_evt_dhcp_abs_leases.event_id = event_id);

DELETE FROM n_router_dhcp_abs_lease
    WHERE NOT EXISTS
        (SELECT 1 FROM n_router_evt_dhcp_abs_leases
            WHERE n_router_dhcp_abs_lease.event_id = lease_id);
