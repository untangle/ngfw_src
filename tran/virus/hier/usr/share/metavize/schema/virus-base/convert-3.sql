-- convert script for release 2.5 (aka 1.5)

-- add mail settings

CREATE TABLE tr_virus_smtp_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    msg_action char(1) NOT NULL,
    notify_action char(1) NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

CREATE TABLE tr_virus_pop_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    msg_action char(1) NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

CREATE TABLE tr_virus_imap_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    msg_action char(1) NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

-- smtp inbound settings
ALTER TABLE tr_virus_settings ADD COLUMN smtp_inbound int8;
UPDATE tr_virus_settings SET smtp_inbound = nextval('hibernate_sequence');
INSERT INTO tr_virus_smtp_config (
    SELECT smtp_inbound, true, 'R', 'N', 'Scan incoming SMTP e-mail'
    FROM tr_virus_settings
);
ALTER TABLE tr_virus_settings ALTER COLUMN smtp_inbound SET NOT NULL;

-- smtp outbound settings
ALTER TABLE tr_virus_settings ADD COLUMN smtp_outbound int8;
UPDATE tr_virus_settings SET smtp_outbound = nextval('hibernate_sequence');
INSERT INTO tr_virus_smtp_config (
    SELECT smtp_outbound, false, 'P', 'N', 'Scan outgoing SMTP e-mail'
    FROM tr_virus_settings

ALTER TABLE tr_virus_settings ALTER COLUMN smtp_outbound SET NOT NULL;

-- pop inbound settings
ALTER TABLE tr_virus_settings ADD COLUMN pop_inbound int8;
UPDATE tr_virus_settings SET pop_inbound = nextval('hibernate_sequence');
INSERT INTO tr_virus_pop_config (
    SELECT pop_inbound, true, 'R', 'Scan incoming POP e-mail'
    FROM tr_virus_settings
);
ALTER TABLE tr_virus_settings ALTER COLUMN pop_inbound SET NOT NULL;

-- pop outbound settings
ALTER TABLE tr_virus_settings ADD COLUMN pop_outbound int8;
UPDATE tr_virus_settings SET pop_outbound = nextval('hibernate_sequence');
INSERT INTO tr_virus_pop_config (
    SELECT pop_outbound, false, 'P', 'Scan outgoing POP e-mail'
    FROM tr_virus_settings
);
ALTER TABLE tr_virus_settings ALTER COLUMN pop_outbound SET NOT NULL;

-- imap inbound settings
ALTER TABLE tr_virus_settings ADD COLUMN imap_inbound int8;
UPDATE tr_virus_settings SET imap_inbound = nextval('hibernate_sequence');
INSERT INTO tr_virus_imap_config (
    SELECT imap_inbound, true, 'R', 'Scan incoming IMAP e-mail'
    FROM tr_virus_settings
);
ALTER TABLE tr_virus_settings ALTER COLUMN imap_inbound SET NOT NULL;

-- imap outbound settings
ALTER TABLE tr_virus_settings ADD COLUMN imap_outbound int8;
UPDATE tr_virus_settings SET imap_outbound = nextval('hibernate_sequence');
INSERT INTO tr_virus_imap_config (
    SELECT imap_outbound, false, 'P', 'Scan outgoing IMAP e-mail'
    FROM tr_virus_settings
);
ALTER TABLE tr_virus_settings ALTER COLUMN imap_outbound SET NOT NULL;

-- add mail events

CREATE TABLE tr_virus_evt_smtp (
    event_id int8 NOT NULL,
    msg_id int8,
    clean bool,
    virus_name varchar(255),
    virus_cleaned bool,
    msg_action char(1),
    notify_action char(1),
    vendor_name varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE tr_virus_evt_mail (
    event_id int8 NOT NULL,
    msg_id int8,
    clean bool,
    virus_name varchar(255),
    virus_cleaned bool,
    msg_action char(1),
    vendor_name varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- add vendor name
ALTER TABLE tr_virus_evt ADD COLUMN vendor_name varchar(255);
UPDATE tr_virus_evt SET vendor_name = 'Clam';

ALTER TABLE tr_virus_evt_http ADD COLUMN vendor_name varchar(255);
UPDATE tr_virus_evt_http SET vendor_name = 'Clam';

-- indexes

CREATE INDEX tr_virus_evt_sid_idx ON tr_virus_evt (session_id);
CREATE INDEX tr_virus_evt_http_rid_idx ON tr_virus_evt_http (request_line);
CREATE INDEX tr_virus_evt_mail_mid_idx ON tr_virus_evt_mail (msg_id);
CREATE INDEX tr_virus_evt_smtp_mid_idx ON tr_virus_evt_smtp (msg_id);

-- foreign key constraints

ALTER TABLE tr_virus_settings
    ADD CONSTRAINT FK_IN_VS_SMTP_CFG
    FOREIGN KEY (smtp_inbound)
    REFERENCES tr_virus_smtp_config;

ALTER TABLE tr_virus_settings
    ADD CONSTRAINT FK_OUT_VS_SMTP_CFG
    FOREIGN KEY (smtp_outbound)
    REFERENCES tr_virus_smtp_config;

ALTER TABLE tr_virus_settings
    ADD CONSTRAINT FK_IN_VS_POP_CFG
    FOREIGN KEY (pop_inbound)
    REFERENCES tr_virus_pop_config;

ALTER TABLE tr_virus_settings
    ADD CONSTRAINT FK_OUT_VS_POP_CFG
    FOREIGN KEY (pop_outbound)
    REFERENCES tr_virus_pop_config;

ALTER TABLE tr_virus_settings
    ADD CONSTRAINT FK_IN_VS_IMAP_CFG
    FOREIGN KEY (imap_inbound)
    REFERENCES tr_virus_imap_config;

ALTER TABLE tr_virus_settings
    ADD CONSTRAINT FK_OUT_VS_IMAP_CFG
    FOREIGN KEY (imap_outbound)
    REFERENCES tr_virus_imap_config;
