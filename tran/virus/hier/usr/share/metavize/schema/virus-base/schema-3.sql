-- schema for release 2.5 (aka 1.5)

CREATE TABLE tr_virus_vs_ext (
    settings_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

CREATE TABLE tr_virus_config (
    config_id int8 NOT NULL,
    scan bool,
    copy_on_block bool,
    notes varchar(255),
    copy_on_block_notes varchar(255),
    PRIMARY KEY (config_id));

CREATE TABLE tr_virus_smtp_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    msg_action char(1) NOT NULL,
    notify_action char(1) NOT NULL,
    copy_on_block bool NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

CREATE TABLE tr_virus_pop_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    msg_action char(1) NOT NULL,
    copy_on_block bool NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

CREATE TABLE tr_virus_imap_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    msg_action char(1) NOT NULL,
    copy_on_block bool NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

CREATE TABLE tr_virus_vs_mt (
    settings_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

CREATE TABLE tr_virus_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    disable_ftp_resume bool,
    disable_http_resume bool,
    trickle_percent int4,
    http_inbound int8 NOT NULL,
    http_outbound int8 NOT NULL,
    ftp_inbound int8 NOT NULL,
    ftp_outbound int8 NOT NULL,
    smtp_inbound int8 NOT NULL,
    smtp_outbound int8 NOT NULL,
    pop_inbound int8 NOT NULL,
    pop_outbound int8 NOT NULL,
    imap_inbound int8 NOT NULL,
    imap_outbound int8 NOT NULL,
    ftp_disable_resume_details varchar(255),
    http_disable_resume_details varchar(255),
    trickle_percent_details varchar(255),
    PRIMARY KEY (settings_id));

CREATE TABLE tr_virus_evt (
    event_id int8 NOT NULL,
    session_id int4,
    clean bool,
    virus_name varchar(255),
    virus_cleaned bool,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE tr_virus_evt_http (
    event_id int8 NOT NULL,
    request_line int8,
    clean bool,
    virus_name varchar(255),
    virus_cleaned bool,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

ALTER TABLE tr_virus_vs_ext ADD CONSTRAINT FKC3BCA54279192AB7 FOREIGN KEY (settings_id) REFERENCES tr_virus_settings;

ALTER TABLE tr_virus_vs_mt ADD CONSTRAINT FKD4C4064679192AB7 FOREIGN KEY (settings_id) REFERENCES tr_virus_settings;

ALTER TABLE tr_virus_settings ADD CONSTRAINT FK98F4CB268403454D FOREIGN KEY (ftp_outbound) REFERENCES tr_virus_config;

ALTER TABLE tr_virus_settings ADD CONSTRAINT FK98F4CB26AD94C6A2 FOREIGN KEY (http_inbound) REFERENCES tr_virus_config;

ALTER TABLE tr_virus_settings ADD CONSTRAINT FK98F4CB265F1C7D5C FOREIGN KEY (ftp_inbound) REFERENCES tr_virus_config;

ALTER TABLE tr_virus_settings ADD CONSTRAINT FK98F4CB261446F FOREIGN KEY (tid) REFERENCES tid;

ALTER TABLE tr_virus_settings ADD CONSTRAINT FK98F4CB2649424C7 FOREIGN KEY (http_outbound) REFERENCES tr_virus_config;

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
