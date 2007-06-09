-- convert script for release 2.5

---------------
-- new tables |
---------------

-- com.untangle.tran.virus.VirusSMTPConfig
CREATE TABLE settings.tr_virus_smtp_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    action char(1) NOT NULL,
    notify_action char(1) NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

-- com.untangle.tran.virus.VirusPOPConfig
CREATE TABLE settings.tr_virus_pop_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    action char(1) NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

-- com.untangle.tran.virus.VirusIMAPConfig
CREATE TABLE settings.tr_virus_imap_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    action char(1) NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

-- com.untangle.tran.virus.VirusSmtpEvent
CREATE TABLE events.tr_virus_evt_smtp (
    event_id int8 NOT NULL,
    msg_id int8,
    clean bool,
    virus_name varchar(255),
    virus_cleaned bool,
    action char(1),
    notify_action char(1),
    vendor_name varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.virus.VirusMailEvent
CREATE TABLE events.tr_virus_evt_mail (
    event_id int8 NOT NULL,
    msg_id int8,
    clean bool,
    virus_name varchar(255),
    virus_cleaned bool,
    action char(1),
    vendor_name varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-----------------------------------
-- move old tables to new schemas |
-----------------------------------

-- com.untangle.tran.virus.VirusSettings
-- (adding smtp_inbound, smtp_outbound, pop_inbound, pop_outbound,
--  imap_inbound, imap_outbound)
CREATE TABLE settings.tr_virus_settings (
    settings_id,
    tid,
    disable_ftp_resume,
    disable_http_resume,
    trickle_percent,
    http_inbound,
    http_outbound,
    ftp_inbound,
    ftp_outbound,
    smtp_inbound,
    smtp_outbound,
    pop_inbound,
    pop_outbound,
    imap_inbound,
    imap_outbound,
    ftp_disable_resume_details,
    http_disable_resume_details,
    trickle_percent_details)
AS SELECT settings_id, tid, disable_ftp_resume, disable_http_resume,
          trickle_percent, http_inbound, http_outbound, ftp_inbound,
          ftp_outbound, null::int8, null::int8, null::int8, null::int8,
          null::int8, null::int8, ftp_disable_resume_details,
          http_disable_resume_details, trickle_percent_details
   FROM public.tr_virus_settings;

-- SMTP inbound settings
UPDATE settings.tr_virus_settings
    SET smtp_inbound = nextval('hibernate_sequence');
INSERT INTO settings.tr_virus_smtp_config (
    SELECT smtp_inbound, true, 'R', 'N', 'Scan incoming SMTP e-mail on inbound sessions'
    FROM settings.tr_virus_settings
);

-- SMTP outbound settings
UPDATE settings.tr_virus_settings
    SET smtp_outbound = nextval('hibernate_sequence');
INSERT INTO settings.tr_virus_smtp_config (
    SELECT smtp_outbound, false, 'P', 'N', 'Scan outgoing SMTP e-mail on outbound sessions'
    FROM settings.tr_virus_settings
);

-- POP inbound settings
UPDATE settings.tr_virus_settings
    SET pop_inbound = nextval('hibernate_sequence');
INSERT INTO settings.tr_virus_pop_config (
    SELECT pop_inbound, true, 'R', 'Scan incoming POP e-mail on outbound sessions'
    FROM settings.tr_virus_settings
);

-- POP outbound settings
UPDATE settings.tr_virus_settings
    SET pop_outbound = nextval('hibernate_sequence');
INSERT INTO settings.tr_virus_pop_config (
    SELECT pop_outbound, false, 'P', 'Scan outgoing POP e-mail on inbound sessions'
    FROM settings.tr_virus_settings
);

-- IMAP inbound settings
UPDATE settings.tr_virus_settings
    SET imap_inbound = nextval('hibernate_sequence');
INSERT INTO settings.tr_virus_imap_config (
    SELECT imap_inbound, true, 'R', 'Scan incoming IMAP e-mail on outbound sessions'
    FROM settings.tr_virus_settings
);

-- IMAP outbound settings
UPDATE settings.tr_virus_settings
    SET imap_outbound = nextval('hibernate_sequence');
INSERT INTO settings.tr_virus_imap_config (
    SELECT imap_outbound, false, 'P', 'Scan outgoing IMAP e-mail on inbound sessions'
    FROM settings.tr_virus_settings
);

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT tr_virus_settings_pkey PRIMARY KEY (settings_id);
ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT tr_virus_settings_uk UNIQUE (tid);
ALTER TABLE settings.tr_virus_settings
    ALTER COLUMN settings_id SET NOT NULL;
ALTER TABLE settings.tr_virus_settings
    ALTER COLUMN tid SET NOT NULL;
ALTER TABLE settings.tr_virus_settings
    ALTER COLUMN http_inbound SET NOT NULL;
ALTER TABLE settings.tr_virus_settings
    ALTER COLUMN http_outbound SET NOT NULL;
ALTER TABLE settings.tr_virus_settings
    ALTER COLUMN ftp_inbound SET NOT NULL;
ALTER TABLE settings.tr_virus_settings
    ALTER COLUMN ftp_outbound SET NOT NULL;
ALTER TABLE settings.tr_virus_settings
    ALTER COLUMN smtp_inbound SET NOT NULL;
ALTER TABLE settings.tr_virus_settings
    ALTER COLUMN smtp_outbound SET NOT NULL;
ALTER TABLE settings.tr_virus_settings
    ALTER COLUMN pop_inbound SET NOT NULL;
ALTER TABLE settings.tr_virus_settings
    ALTER COLUMN pop_outbound SET NOT NULL;
ALTER TABLE settings.tr_virus_settings
    ALTER COLUMN imap_inbound SET NOT NULL;
ALTER TABLE settings.tr_virus_settings
    ALTER COLUMN imap_outbound SET NOT NULL;

-- com.untangle.tran.virus.VirusSettings.extensions
CREATE TABLE settings.tr_virus_vs_ext
    AS SELECT * FROM public.tr_virus_vs_ext;

ALTER TABLE settings.tr_virus_vs_ext
    ADD CONSTRAINT tr_virus_vs_ext_pkey
    PRIMARY KEY (settings_id, position);
ALTER TABLE settings.tr_virus_vs_ext
    ALTER COLUMN settings_id SET NOT NULL;
ALTER TABLE settings.tr_virus_vs_ext
    ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.tr_virus_vs_ext
    ALTER COLUMN position SET NOT NULL;

-- com.untangle.tran.virus.VirusConfig
CREATE TABLE settings.tr_virus_config
    AS SELECT * FROM public.tr_virus_config;

ALTER TABLE settings.tr_virus_config
    ADD CONSTRAINT tr_virus_config_pkey
    PRIMARY KEY (config_id);
ALTER TABLE settings.tr_virus_config
    ALTER COLUMN config_id SET NOT NULL;

-- com.untangle.tran.virus.VirusSettings.httpMimeTypes
CREATE TABLE settings.tr_virus_vs_mt
    AS SELECT * FROM public.tr_virus_vs_mt;

ALTER TABLE settings.tr_virus_vs_mt
    ADD CONSTRAINT tr_virus_vs_mt_pkey
    PRIMARY KEY (settings_id, position);
ALTER TABLE settings.tr_virus_vs_mt
    ALTER COLUMN settings_id SET NOT NULL;
ALTER TABLE settings.tr_virus_vs_mt
    ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.tr_virus_vs_mt
    ALTER COLUMN position SET NOT NULL;

-- com.untangle.tran.virus.VirusLogEvent

-- add vendor name
CREATE TABLE events.tr_virus_evt
    AS SELECT event_id, session_id, clean, virus_name, virus_cleaned,
              'Clam'::varchar(255) AS vendor_name, time_stamp
       FROM public.tr_virus_evt;

ALTER TABLE events.tr_virus_evt
    ADD CONSTRAINT tr_virus_evt_pkey
    PRIMARY KEY (event_id);
ALTER TABLE events.tr_virus_evt
    ALTER COLUMN event_id SET NOT NULL;

-- com.untangle.tran.virus.VirusHttpEvent

CREATE TABLE events.tr_virus_evt_http
    AS SELECT event_id, request_line, clean, virus_name, virus_cleaned,
              'Clam'::varchar(255) AS vendor_name, time_stamp
       FROM public.tr_virus_evt_http;

ALTER TABLE events.tr_virus_evt_http
    ADD CONSTRAINT tr_virus_evt_http_pkey
    PRIMARY KEY (event_id);
ALTER TABLE events.tr_virus_evt_http
    ALTER COLUMN event_id SET NOT NULL;

-------------------------
-- recreate constraints |
-------------------------

-- indeces

CREATE INDEX tr_virus_evt_sid_idx
    ON events.tr_virus_evt (session_id);
CREATE INDEX tr_virus_evt_http_rid_idx
    ON events.tr_virus_evt_http (request_line);
CREATE INDEX tr_virus_evt_mail_mid_idx
    ON events.tr_virus_evt_mail (msg_id);
CREATE INDEX tr_virus_evt_smtp_mid_idx
    ON events.tr_virus_evt_smtp (msg_id);

-- foreign key constraints

ALTER TABLE settings.tr_virus_vs_ext
    ADD CONSTRAINT fk_tr_virus_vs_ext
    FOREIGN KEY (settings_id)
    REFERENCES settings.tr_virus_settings;

ALTER TABLE settings.tr_virus_vs_mt
    ADD CONSTRAINT fk_tr_virus_vs_mt
    FOREIGN KEY (settings_id)
    REFERENCES settings.tr_virus_settings;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings
    FOREIGN KEY (tid)
    REFERENCES settings.tid;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_ftpout
    FOREIGN KEY (ftp_outbound)
    REFERENCES settings.tr_virus_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_ftpin
    FOREIGN KEY (ftp_inbound)
    REFERENCES settings.tr_virus_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_httpout
    FOREIGN KEY (http_outbound)
    REFERENCES settings.tr_virus_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_set_httpin
    FOREIGN KEY (http_inbound)
    REFERENCES settings.tr_virus_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_smtpout
    FOREIGN KEY (smtp_outbound)
    REFERENCES settings.tr_virus_smtp_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_smtpin
    FOREIGN KEY (smtp_inbound)
    REFERENCES settings.tr_virus_smtp_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_popout
    FOREIGN KEY (pop_outbound)
    REFERENCES settings.tr_virus_pop_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_popin
    FOREIGN KEY (pop_inbound)
    REFERENCES settings.tr_virus_pop_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_imapout
    FOREIGN KEY (imap_outbound)
    REFERENCES settings.tr_virus_imap_config;

ALTER TABLE settings.tr_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_imapin
    FOREIGN KEY (imap_inbound)
    REFERENCES settings.tr_virus_imap_config;

-------------------------
-- drop old constraints |
-------------------------

-- foreign key constraints

ALTER TABLE tr_virus_vs_ext DROP CONSTRAINT fkc3bca54279192ab7;
ALTER TABLE tr_virus_vs_mt DROP CONSTRAINT fkd4c4064679192ab7;
ALTER TABLE tr_virus_settings DROP CONSTRAINT fk98f4cb268403454d;
ALTER TABLE tr_virus_settings DROP CONSTRAINT fk98f4cb26ad94c6a2;
ALTER TABLE tr_virus_settings DROP CONSTRAINT fk98f4cb265f1c7d5c;
ALTER TABLE tr_virus_settings DROP CONSTRAINT fk98f4cb261446f;
ALTER TABLE tr_virus_settings DROP CONSTRAINT fk98f4cb2649424c7;

--------------------
-- drop old tables |
--------------------

DROP TABLE public.tr_virus_vs_ext;
DROP TABLE public.tr_virus_config;
DROP TABLE public.tr_virus_vs_mt;
DROP TABLE public.tr_virus_settings;
DROP TABLE public.tr_virus_evt;
DROP TABLE public.tr_virus_evt_http;

------------
-- analyze |
------------

ANALYZE events.tr_virus_evt;
ANALYZE events.tr_virus_evt_http;
