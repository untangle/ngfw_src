-- schema for release-3.1

-------------
-- settings |
-------------

create table tr_openvpn_c_site_network (
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

create table tr_openvpn_s_site_network (
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

create table tr_openvpn_client (
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
        primary key (RULE_ID));

create table tr_openvpn_group (
        RULE_ID int8 not null,
        client_pool_address inet,
        client_pool_netmask inet,
        NAME text,
        CATEGORY text,
        DESCRIPTION text,
        LIVE bool,
        ALERT bool,
        LOG bool,
        settings_id int8,
        position int4,
        primary key (RULE_ID));

create table tr_openvpn_settings (
        ID int8 not null,
        TID int8 not null,
        domain text,
        key_size int4,
        country text,
        province text,
        locality text,
        org text,
        org_unit text,
        email text,
        is_ca_on_usb bool,
        primary key (ID));

alter table tr_openvpn_c_site_network add constraint FKF75374E830D9EF2D
        foreign key (client_id) references tr_openvpn_client;

alter table tr_openvpn_s_site_network add constraint FKF75374E89E4538C5
        foreign key (settings_id) references tr_openvpn_settings;

alter table tr_openvpn_client add constraint FKF4113219E4538C5
        foreign key (settings_id) references tr_openvpn_settings;

alter table tr_openvpn_group add constraint FKB66694699E4538C5
        foreign key (settings_id) references tr_openvpn_settings;

alter table tr_openvpn_settings add constraint FK62A65DF939CBD260
        foreign key (TID) references TID;
