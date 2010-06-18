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

CREATE TABLE settings.n_openvpn_c_site_network (
        rule_id INT8 NOT NULL,
        network INET,
        netmask INET,
        "name" TEXT,
        category TEXT,
        description TEXT,
        live BOOL,
        alert BOOL,
        log BOOL,
        client_id INT8,
        "position" INT4,
        PRIMARY KEY (rule_id));

CREATE TABLE settings.n_openvpn_s_site_network (
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

CREATE TABLE settings.n_openvpn_client_2 (
        rule_id INT8 NOT NULL,
        address INET,
        is_edgeguard BOOL,
        group_name TEXT,
        "name" TEXT,
        category TEXT,
        description TEXT,
        live BOOL,
        alert bool,
        log BOOL,
        settings_id INT8,
        "position" INT4,
        dist_key TEXT,
        dist_passwd TEXT,
        PRIMARY KEY (rule_id));

CREATE TABLE settings.n_openvpn_site_2 (
        rule_id INT8 NOT NULL,
        address INET,
        is_edgeguard BOOL,
        group_name TEXT,
        "name" TEXT,
        category TEXT,
        description TEXT,
        live BOOL,
        alert BOOL,
        log BOOL,
        settings_id INT8,
        "position" INT4,
        dist_key TEXT,
        dist_passwd TEXT,
        PRIMARY KEY (rule_id));

CREATE TABLE settings.n_openvpn_group (
        rule_id INT8 not null,
        address INET,
        netmask INET,
        NAME text,
        CATEGORY text,
        DESCRIPTION text,
        LIVE bool,
        ALERT bool,
        LOG bool,
        settings_id int8,
        position int4,
        use_dns bool,
        primary key (RULE_ID));

CREATE TABLE settings.n_openvpn_settings (
        id INT8 not null,
        tid INT8 not null,
        server_address TEXT,
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
        public_port int4,
        is_dns_override     BOOL,
        dns_1               INET,
        dns_2               INET,
        site_name           TEXT,
        primary key (ID));

ALTER TABLE n_openvpn_c_site_network ADD CONSTRAINT n_openvpn_c_site_network_to_site
        FOREIGN KEY (client_id) REFERENCES n_openvpn_site_2;

ALTER TABLE n_openvpn_s_site_network ADD CONSTRAINT FKF75374E89E4538C5
        FOREIGN KEY (settings_id) REFERENCES n_openvpn_settings;

ALTER TABLE n_openvpn_client_2 ADD CONSTRAINT FKF4113219E4538C5
        FOREIGN KEY (settings_id) REFERENCES n_openvpn_settings;

ALTER TABLE n_openvpn_site_2 ADD CONSTRAINT n_openvpn_site_to_settings
        FOREIGN KEY (settings_id) REFERENCES n_openvpn_settings;

ALTER TABLE n_openvpn_group ADD CONSTRAINT FKB66694699E4538C5
        FOREIGN KEY (settings_id) REFERENCES n_openvpn_settings;

ALTER TABLE n_openvpn_settings ADD CONSTRAINT FK62A65DF939CBD260
        FOREIGN KEY (tid) REFERENCES u_tid;
