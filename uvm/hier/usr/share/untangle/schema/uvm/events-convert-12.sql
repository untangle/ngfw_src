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

ALTER TABLE events.mvvm_login_evt RENAME TO u_login_evt;
ALTER TABLE events.transform_state_change RENAME TO u_node_state_change;
ALTER TABLE events.pl_endp RENAME TO pl_endp;
ALTER TABLE events.pl_stats RENAME TO pl_stats;
ALTER TABLE events.shield_rejection_evt RENAME TO n_shield_rejection_evt;
ALTER TABLE events.shield_statistic_evt RENAME TO n_shield_statistic_evt;
ALTER TABLE events.portal_login_evt RENAME TO n_portal_login_evt;
ALTER TABLE events.portal_logout_evt RENAME TO n_portal_logout_evt;
ALTER TABLE events.portal_app_launch_evt RENAME TO n_portal_app_launch_evt;
ALTER TABLE events.mvvm_lookup_evt RENAME TO u_lookup_evt;

-- Indexes
ALTER TABLE events.mvvm_login_evt_ts_idx RENAME TO u_login_evt_ts_idx;
ALTER TABLE events.mvvm_lookup_evt_ts_idx RENAME TO u_lookup_evt_ts_idx;
ALTER TABLE events.shield_rejection_evt_ts_idx RENAME TO n_shield_rejection_evt_ts_idx;
ALTER TABLE events.portal_login_evt_ts_idx RENAME TO n_portal_login_evt_ts_idx;
ALTER TABLE events.portal_logout_evt_ts_idx RENAME TO n_portal_logout_evt_ts_idx;

ALTER TABLE portal_login_evt_ts_idx RENAME TO n_portal_login_evt_ts_idx;
ALTER TABLE portal_logout_evt_ts_idx RENAME TO n_portal_logout_evt_ts_idx;
