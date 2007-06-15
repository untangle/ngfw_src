-- events conversion for release-5.0
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

ALTER TABLE events.tr_nat_evt_dhcp RENAME TO n_router_evt_dhcp;
ALTER TABLE events.dhcp_abs_lease RENAME TO n_router_dhcp_abs_lease;
ALTER TABLE events.tr_nat_evt_dhcp_abs RENAME TO n_router_evt_dhcp_abs;
ALTER TABLE events.tr_nat_evt_dhcp_abs_leases RENAME TO n_router_evt_dhcp_abs_leases;
ALTER TABLE events.tr_nat_redirect_evt RENAME TO n_router_redirect_evt;
ALTER TABLE events.tr_nat_statistic_evt RENAME TO n_router_statistic_evt;
