-- events conversion for release-4.0
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

-- com.untangle.mvvm.portal.PortalLoginEvent
CREATE TABLE events.portal_login_evt (
    event_id    int8 NOT NULL,
    client_addr inet,
    uid         text,
    succeeded   bool,
    reason      char(1),
    time_stamp  timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.mvvm.portal.PortalLogoutEvent
CREATE TABLE events.portal_logout_evt (
    event_id    int8 NOT NULL,
    client_addr inet,
    uid         text,
    reason      char(1),
    time_stamp  timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.mvvm.portal.PortalAppLaunchEvent
CREATE TABLE events.portal_app_launch_evt (
    event_id    int8 NOT NULL,
    client_addr inet,
    uid         text,
    succeeded   bool,
    reason      char(1),
    app         text,
    destination text,
    time_stamp  timestamp,
    PRIMARY KEY (event_id));
