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
	VARIABLE varchar(512),
	DEFINITION varchar(512),
	DESCRIPTION varchar(1024),
	SETTINGS_ID int8,
	POSITION int4,
	primary key (VARIABLE_ID));

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
	RULE varchar(4095),
	NAME varchar(255),
	CATEGORY varchar(255),
	DESCRIPTION varchar(255),
	LIVE bool,
	ALERT bool,
	LOG bool,
	SETTINGS_ID int8, 
	POSITION int4, 
	primary key (RULE_ID));

-----------
-- events |
-----------
create table events.TR_IDS_EVT (
	event_id int8 NOT NULL,
	session_id int4,
	message char(512),
	blocked bool,
	time_stamp timestamp,
	PRIMARY KEY (event_id));
