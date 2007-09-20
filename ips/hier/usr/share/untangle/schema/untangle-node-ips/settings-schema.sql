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

create table settings.n_ips_settings (
    settings_id int8 not null,
    max_chunks int8 not null,
    tid int8 not null unique,
    primary key (settings_id));

create table settings.n_ips_variable (
    VARIABLE_ID int8 not null,
    VARIABLE text,
    DEFINITION text,
    DESCRIPTION text,
    SETTINGS_ID int8,
    POSITION int4,
    PRIMARY KEY (VARIABLE_ID));

create table settings.n_ips_immutable_variables (
    setting_id int8 NOT NULL,
    variable_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

create table settings.n_ips_mutable_variables (
    setting_id int8 NOT NULL,
    variable_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

create table settings.n_ips_rule (
    RULE_ID int8 not null,
    RULE text,
    SID int4,
    NAME text,
    CATEGORY text,
    DESCRIPTION text,
    LIVE bool,
    ALERT bool,
    LOG bool,
    SETTINGS_ID int8,
    POSITION int4,
    primary key (RULE_ID));
