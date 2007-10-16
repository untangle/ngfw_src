-- settings conversion for release 4.2
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

UPDATE settings.user_policy_rule SET user_matcher = '[any]' WHERE user_matcher = 'any';

-- com.untangle.mvvm.networking.AccessSettings -- 4.2
CREATE TABLE settings.mvvm_access_settings (
    settings_id          INT8 NOT NULL,
    allow_ssh            BOOL,
    allow_insecure       BOOL,
    allow_outside        BOOL,
    restrict_outside     BOOL,
    outside_network      INET,
    outside_netmask      INET,
    allow_outside_admin  BOOL,
    allow_outside_quaran BOOL,
    allow_outside_report BOOL,
    PRIMARY KEY          (settings_id));

-- com.untangle.mvvm.networking.MiscSettings -- 4.2
CREATE TABLE settings.mvvm_misc_settings (
    settings_id          INT8 NOT NULL,
    report_exceptions    BOOL,
    tcp_window_scaling   BOOL,
    post_configuration   TEXT,
    custom_rules         TEXT,
    PRIMARY KEY          (settings_id));

-- com.untangle.mvvm.networking.AddressSettings -- 4.2
CREATE TABLE settings.mvvm_address_settings (
    settings_id          INT8 NOT NULL,
    https_port           INT4,
    hostname             TEXT,
    is_hostname_public   BOOL,
    has_public_address   BOOL,
    public_ip_addr       INET,
    public_port          INT4,
    PRIMARY KEY          (settings_id));
