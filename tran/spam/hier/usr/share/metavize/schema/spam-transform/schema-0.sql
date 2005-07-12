-- schema for release 2.5 (aka 1.5)

CREATE TABLE tr_spam_smtp_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    msg_action char(1) NOT NULL,
    notify_action char(1) NOT NULL,
    copy_on_block bool NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

CREATE TABLE tr_spam_pop_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    msg_action char(1) NOT NULL,
    copy_on_block bool NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

CREATE TABLE tr_spam_imap_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    msg_action char(1) NOT NULL,
    copy_on_block bool NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

CREATE TABLE tr_spam_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    smtp_inbound int8 NOT NULL,
    smtp_outbound int8 NOT NULL,
    pop_inbound int8 NOT NULL,
    pop_outbound int8 NOT NULL,
    imap_inbound int8 NOT NULL,
    imap_outbound int8 NOT NULL,
    PRIMARY KEY (settings_id));

ALTER TABLE tr_spam_settings
    ADD CONSTRAINT FK_IN_SS_SMTP_CFG
    FOREIGN KEY (smtp_inbound)
    REFERENCES tr_spam_smtp_config;

ALTER TABLE tr_spam_settings
    ADD CONSTRAINT FK_OUT_SS_SMTP_CFG
    FOREIGN KEY (smtp_outbound)
    REFERENCES tr_spam_smtp_config;

ALTER TABLE tr_spam_settings
    ADD CONSTRAINT FK_IN_SS_POP_CFG
    FOREIGN KEY (pop_inbound)
    REFERENCES tr_spam_pop_config;

ALTER TABLE tr_spam_settings
    ADD CONSTRAINT FK_OUT_SS_POP_CFG
    FOREIGN KEY (pop_outbound)
    REFERENCES tr_spam_pop_config;

ALTER TABLE tr_spam_settings
    ADD CONSTRAINT FK_IN_SS_IMAP_CFG
    FOREIGN KEY (imap_inbound)
    REFERENCES tr_spam_imap_config;

ALTER TABLE tr_spam_settings
    ADD CONSTRAINT FK_OUT_SS_IMAP_CFG
    FOREIGN KEY (imap_outbound)
    REFERENCES tr_spam_imap_config;
