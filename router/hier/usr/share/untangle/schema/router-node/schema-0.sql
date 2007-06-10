create table DHCP_LEASE_RULE (
        RULE_ID int8 not null, 
        MAC_ADDRESS varchar(255),
        HOSTNAME varchar(255),
        STATIC_ADDRESS inet, 
        IS_RESOLVE_MAC bool, 
        NAME varchar(255), 
        CATEGORY varchar(255), 
        DESCRIPTION varchar(255), 
        LIVE bool, 
        ALERT bool, 
        LOG bool, 
        primary key (RULE_ID));

create table TR_DHCP_LEASES (
        SETTING_ID int8 not null, 
        RULE_ID int8 not null, 
        POSITION int4 not null, 
        primary key (SETTING_ID, POSITION));


create table REDIRECT_RULE (
        RULE_ID int8 not null,
        IS_DST_REDIRECT bool,
        REDIRECT_PORT int4,
        REDIRECT_ADDR inet,
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


create table TR_NAT_SETTINGS (
        SETTINGS_ID int8 not null,
        TID int8 not null unique, 
        NAT_ENABLED bool, 
        NAT_INTERNAL_ADDR inet, 
        NAT_INTERNAL_SUBNET inet, 
        DMZ_ENABLED bool, 
        DMZ_ADDRESS inet, 
        DHCP_ENABLED bool, 
        DHCP_S_ADDRESS inet, 
        DHCP_E_ADDRESS inet, 
        DHCP_LEASE_TIME int4, 
        DNS_ENABLED bool,
        DNS_LOCAL_DOMAIN varchar(255), 
        primary key (SETTINGS_ID));

create table TR_NAT_SETTINGS (
        SETTINGS_ID int8 not null,
        TID int8 not null unique, 
        NAT_ENABLED bool, 
        NAT_INTERNAL_ADDR inet,
        NAT_INTERNAL_SUBNET inet,
        DMZ_ENABLED bool,
        DMZ_ADDRESS inet,
        DNS_MASQ_EN bool,
        primary key (SETTINGS_ID));

create table TR_NAT_REDIRECTS (
        SETTING_ID int8 not null,
        RULE_ID int8 not null,
        POSITION int4 not null,
        primary key (SETTING_ID, POSITION));

alter table TR_NAT_SETTINGS add constraint FK2F819DC21446F foreign key (TID) references TID;

alter table TR_NAT_REDIRECTS add constraint FKCBBF56381CAE658A foreign key (SETTING_ID) references TR_NAT_SETTINGS;
alter table TR_NAT_REDIRECTS add constraint FKCBBF5638871AAD3E foreign key (RULE_ID) references REDIRECT_RULE;
alter table TR_DHCP_LEASES add constraint FKA6469261CAE658A foreign key (SETTING_ID) references TR_NAT_SETTINGS;
alter table TR_DHCP_LEASES add constraint FKA646926871AAD3E foreign key (RULE_ID) references DHCP_LEASE_RULE;
