-- settings schema for release-5.0
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

-------------
-- settings |
-------------

-- com.untangle.tran.airgap.AirgapSettings
CREATE TABLE settings.n_shield_settings (
    settings_id int8 NOT NULL,
    tid         int8 NOT NULL UNIQUE,
    PRIMARY KEY (settings_id));

CREATE TABLE settings.n_shield_node_rule (
    rule_id     INT8 NOT NULL,
    name        text,
    category    text,
    description text,
    live        BOOL NOT NULL,
    alert       BOOL NOT NULL,
    log         BOOL NOT NULL,
    address     INET,
    netmask     INET,
    divider     REAL NOT NULL,
    settings_id INT8,
    position    INT4,
    PRIMARY KEY (rule_id));

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.n_shield_settings
    ADD CONSTRAINT fk_tr_airgap_settings FOREIGN KEY (tid) REFERENCES u_tid;

ALTER TABLE settings.n_shield_node_rule
    ADD CONSTRAINT fk_tr_airgap_shield_node_rule
        FOREIGN KEY (settings_id) REFERENCES settings.n_shield_settings;
