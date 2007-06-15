-- schema for release-3.1
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

create table settings.tr_openvpn_c_site_network (
        RULE_ID int8 not null,
        network inet,
        netmask inet,
        NAME text,
        CATEGORY text,
        DESCRIPTION text,
        LIVE bool,
        ALERT bool,
        LOG bool,
        client_id int8,
        position int4,
        primary key (RULE_ID));

create table settings.tr_openvpn_s_site_network (
        RULE_ID int8 not null,
        network inet,
        netmask inet,
        NAME text,
        CATEGORY text,
        DESCRIPTION text,
        LIVE bool,
        ALERT bool,
        LOG bool,
        settings_id int8,
        position int4,
        primary key (RULE_ID));

create table settings.tr_openvpn_client (
        RULE_ID int8 not null,
        address inet,
        is_edgeguard bool,
        group_id int8,
        NAME text,
        CATEGORY text,
        DESCRIPTION text,
        LIVE bool,
        ALERT bool,
        LOG bool,
        settings_id int8,
        position int4,
        dist_key text,
        dist_passwd text,
        primary key (RULE_ID));

create table settings.tr_openvpn_site (
        RULE_ID int8 not null,
        address inet,
        is_edgeguard bool,
        group_id int8,
        NAME text,
        CATEGORY text,
        DESCRIPTION text,
        LIVE bool,
        ALERT bool,
        LOG bool,
        settings_id int8,
        position int4,
        dist_key text,
        dist_passwd text,
        primary key (RULE_ID));

create table settings.tr_openvpn_group (
        RULE_ID int8 not null,
        address inet,
        netmask inet,
        NAME text,
        CATEGORY text,
        DESCRIPTION text,
        LIVE bool,
        ALERT bool,
        LOG bool,
        settings_id int8,
        position int4,
        primary key (RULE_ID));

create table settings.tr_openvpn_settings (
        ID int8 not null,
        TID int8 not null,
        server_address inet,
        domain text,
        key_size int4,
        country text,
        province text,
        locality text,
        org text,
        org_unit text,
        email text,
        max_clients int4,
        is_edgeguard_client bool,
        is_ca_on_usb bool,
        is_bridge bool,
        expose_clients bool,
        keep_alive bool,
        primary key (ID));

create table events.tr_openvpn_statistic_evt (
        event_id    INT8 NOT NULL,
        time_stamp  TIMESTAMP,
        rx_bytes    INT8,
        tx_bytes    INT8,
        start_time  TIMESTAMP,
        end_time    TIMESTAMP,
        PRIMARY KEY (event_id));

create table events.tr_openvpn_distr_evt (
        event_id INT8 NOT NULL,
        remote_address INET,
        client_name TEXT,
        time_stamp  TIMESTAMP,
        PRIMARY KEY (event_id));

create table events.tr_openvpn_connect_evt (
        event_id INT8 NOT NULL,
        remote_address INET,
        remote_port    INT4,
        client_name    TEXT,
        rx_bytes       INT8,
        tx_bytes       INT8,
        time_stamp     TIMESTAMP,
        start_time     TIMESTAMP,
        end_time       TIMESTAMP,
        PRIMARY KEY    (event_id));

alter table tr_openvpn_c_site_network add constraint FKF75374E830D9EF2D
        foreign key (client_id) references tr_openvpn_site;

alter table tr_openvpn_s_site_network add constraint FKF75374E89E4538C5
        foreign key (settings_id) references tr_openvpn_settings;

alter table tr_openvpn_client add constraint FKF4113219E4538C5
        foreign key (settings_id) references tr_openvpn_settings;

alter table tr_openvpn_site add constraint tr_openvpn_site_to_settings
        foreign key (settings_id) references tr_openvpn_settings;

alter table tr_openvpn_group add constraint FKB66694699E4538C5
        foreign key (settings_id) references tr_openvpn_settings;

alter table tr_openvpn_settings add constraint FK62A65DF939CBD260
        foreign key (TID) references TID;
