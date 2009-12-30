-- schema for release-7.2
-- $HeadURL: svn://chef/work/src/test/hier/usr/share/untangle/schema/untangle-node-test/settings-schema.sql $
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

-------------
-- settings |
-------------

-- com.untangle.node.cpd.CPDRule
CREATE TABLE settings.n_cpd_capture_rule (
    rule_id INT8 NOT NULL,
    settings_id INT8,
    position INT4,
    name TEXT,
    category TEXT,
    description TEXT,
    live BOOL,
    alert BOOL,
    log BOOL,
    capture_enabled BOOL,
    client_interface TEXT,
    client_address TEXT,
    server_address TEXT,
    start_time TEXT,
    end_time TEXT,
    days TEXT,
    PRIMARY KEY (rule_id));


-- com.untangle.node.cpd.CPDSettings
CREATE TABLE settings.n_cpd_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    passed_clients TEXT,
    passed_servers TEXT,
    capture_bypassed_traffic BOOL,
    authentication_type TEXT,
    idle_timeout INT4,
    timeout INT4,
    logout_button BOOL,
    concurrent_logins BOOL,
    page_type TEXT,
    page_parameters TEXT,
    redirect_url TEXT,
    https_page TEXT,
    redirect_https BOOL,
    PRIMARY KEY (settings_id));

----------------
-- constraints |
----------------

-- foreign key constraints
ALTER TABLE settings.n_cpd_settings
    ADD CONSTRAINT fk_tr_cpd_settings
        FOREIGN KEY (tid) REFERENCES settings.u_tid;