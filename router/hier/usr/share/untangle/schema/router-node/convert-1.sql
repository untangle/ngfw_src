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

create table TR_NAT_DNS_HOSTS (
        SETTING_ID int8 not null,
        RULE_ID int8 not null,
        POSITION int4 not null,
        primary key (SETTING_ID, POSITION));


create table DNS_STATIC_HOST_RULE (
        RULE_ID int8 not null,
        HOSTNAME_LIST varchar(255),
        STATIC_ADDRESS inet,
        NAME varchar(255),
        CATEGORY varchar(255),
        DESCRIPTION varchar(255),
        LIVE bool,
        ALERT bool,
        LOG bool,
        primary key (RULE_ID));

create table TR_NAT_EVT_DHCP (
        EVENT_ID int8 not null,
        MAC varchar(255),
        HOSTNAME varchar(255),
        IP inet,
        END_OF_LEASE timestamp,
        EVENT_TYPE int4,
        TIME_STAMP timestamp,
        primary key (EVENT_ID));

create table DHCP_ABS_LEASE (
        EVENT_ID int8 not null,
        MAC varchar(255),
        HOSTNAME varchar(255),
        IP inet, END_OF_LEASE timestamp,
        EVENT_TYPE int4,
        primary key (EVENT_ID));

create table TR_NAT_EVT_DHCP_ABS (
        EVENT_ID int8 not null,
        TIME_STAMP timestamp,
        primary key (EVENT_ID));

create table TR_NAT_EVT_DHCP_ABS_LEASES (
        EVENT_ID int8 not null,
        LEASE_ID int8 not null,
        POSITION int4 not null,
        primary key (EVENT_ID, POSITION));

alter table TR_NAT_DNS_HOSTS add constraint FK956BCB361CAE658A foreign key (SETTING_ID) references TR_NAT_SETTINGS;
alter table TR_NAT_DNS_HOSTS add constraint FK956BCB36871AAD3E foreign key (RULE_ID) references DNS_STATIC_HOST_RULE;
alter table TR_NAT_EVT_DHCP_ABS_LEASES add constraint FK852599793F3A2900 foreign key (EVENT_ID) references TR_NAT_EVT_DHCP_ABS;
alter table TR_NAT_EVT_DHCP_ABS_LEASES add constraint FK852599798C84B540 foreign key (LEASE_ID) references DHCP_ABS_LEASE;

update TR_NAT_SETTINGS set DNS_LOCAL_DOMAIN="";
update TR_NAT_SETTINGS set DHCP_LEASE_TIME=14400;
