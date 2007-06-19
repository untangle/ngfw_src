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

create table FIREWALL_RULE (
        RULE_ID int8 not null,
        IS_TRAFFIC_BLOCKER bool,
        IS_DST_REDIRECT bool,
        PROTOCOL_MATCHER varchar(255),
        SRC_IP_MATCHER varchar(255),
        DST_IP_MATCHER varchar(255),
        SRC_PORT_MATCHER varchar(255),
        DST_PORT_MATCHER varchar(255),
        SRC_INTF_MATCHER varchar(255),
        DST_INTF_MATCHER varchar(255),
        NAME varchar(255),
        CATEGORY varchar(255),
        DESCRIPTION varchar(255),
        LIVE bool,
        ALERT bool,
        LOG bool,
        primary key (RULE_ID));

create table TR_FIREWALL_RULES (
        SETTING_ID int8 not null,
        RULE_ID int8 not null, 
        POSITION int4 not null,
        primary key (SETTING_ID, POSITION));

create table TR_FIREWALL_SETTINGS (
        SETTINGS_ID int8 not null, 
        TID int8 not null unique, 
        IS_QUICKEXIT bool,
        IS_REJECT_SILENT bool, 
        IS_DEFAULT_ACCEPT bool, 
        primary key (SETTINGS_ID));

alter table TR_FIREWALL_RULES add constraint FK4BBFB8B9871AAD3E foreign key (RULE_ID) references FIREWALL_RULE;
alter table TR_FIREWALL_RULES add constraint FK4BBFB8B91CAE658A foreign key (SETTING_ID) references TR_FIREWALL_SETTINGS;
alter table TR_FIREWALL_SETTINGS add constraint FK23CDA1011446F foreign key (TID) references TID;
