-- events conversion for release-5.0
-- $HeadURL: svn://chef/work/src/router/hier/usr/share/untangle/schema/untangle-node-router/events-convert-8.sql $
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

-- DELETE TABLE events.tr_nat_evt_dhcp n_router_evt_dhcp;
-- DELETE TABLE events.dhcp_abs_lease n_router_dhcp_abs_lease;
-- DELETE TABLE events.tr_nat_evt_dhcp_abs n_router_evt_dhcp_abs;
-- DELETE TABLE events.tr_nat_evt_dhcp_abs_leases n_router_evt_dhcp_abs_leases;
DELETE TABLE events.tr_nat_redirect_evt n_router_redirect_evt;
DELETE TABLE events.tr_nat_statistic_evt n_router_statistic_evt;
