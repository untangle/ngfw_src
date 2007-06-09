-- schema for release-4.2

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

create table settings.tr_openvpn_settings (
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

alter table tr_openvpn_c_site_network add constraint FKF75374E830D9EF2D
        foreign key (client_id) references tr_openvpn_site;

ALTER TABLE tr_openvpn_s_site_network ADD CONSTRAINT FKF75374E89E4538C5
        FOREIGN KEY (settings_id) REFERENCES tr_openvpn_settings;

ALTER TABLE tr_openvpn_client ADD CONSTRAINT FKF4113219E4538C5
        FOREIGN KEY (settings_id) REFERENCES tr_openvpn_settings;

ALTER TABLE tr_openvpn_site ADD CONSTRAINT tr_openvpn_site_to_settings
        FOREIGN KEY (settings_id) REFERENCES tr_openvpn_settings;

ALTER TABLE tr_openvpn_group ADD CONSTRAINT FKB66694699E4538C5
        FOREIGN KEY (settings_id) REFERENCES tr_openvpn_settings;

ALTER TABLE tr_openvpn_settings ADD CONSTRAINT FK62A65DF939CBD260
        FOREIGN KEY (tid) REFERENCES tid;
