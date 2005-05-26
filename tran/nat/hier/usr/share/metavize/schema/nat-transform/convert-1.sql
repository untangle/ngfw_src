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

alter table TR_NAT_DNS_HOSTS add constraint FK956BCB361CAE658A foreign key (SETTING_ID) references TR_NAT_SETTINGS;
alter table TR_NAT_DNS_HOSTS add constraint FK956BCB36871AAD3E foreign key (RULE_ID) references DNS_STATIC_HOST_RULE;

update TR_NAT_SETTINGS set DNS_LOCAL_DOMAIN="";
update TR_NAT_SETTINGS set DHCP_LEASE_TIME=14400;
