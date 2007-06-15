-- schema for release-3.3
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

