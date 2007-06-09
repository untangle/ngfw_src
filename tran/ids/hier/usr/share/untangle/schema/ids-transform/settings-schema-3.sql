-- schema for release 4.2

-------------
-- settings |
-------------

create table settings.TR_IDS_SETTINGS (
    settings_id int8 not null,
    max_chunks int8 not null,
    tid int8 not null unique,
    primary key (settings_id));

create table settings.TR_IDS_VARIABLE (
    VARIABLE_ID int8 not null,
    VARIABLE text,
    DEFINITION text,
    DESCRIPTION text,
    SETTINGS_ID int8,
    POSITION int4,
    PRIMARY KEY (VARIABLE_ID));

create table settings.TR_IDS_IMMUTABLE_VARIABLES (
    setting_id int8 NOT NULL,
    variable_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

create table settings.TR_IDS_MUTABLE_VARIABLES (
    setting_id int8 NOT NULL,
    variable_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

create table settings.TR_IDS_RULE (
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
