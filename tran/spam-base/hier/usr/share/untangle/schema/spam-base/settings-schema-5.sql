-- settings schema for release 4.2

-------------
-- settings |
-------------

CREATE TABLE settings.tr_spam_smtp_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    strength int4 NOT NULL,
    msg_size_limit int4 NOT NULL,
    msg_action char(1) NOT NULL,
    notify_action char(1) NOT NULL,
    notes varchar(255),
    throttle boolean NOT NULL,
    throttle_sec int4 NOT NULL,
    PRIMARY KEY (config_id));

CREATE TABLE settings.tr_spam_pop_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    strength int4 NOT NULL,
    msg_size_limit int4 NOT NULL,
    msg_action char(1) NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

CREATE TABLE settings.tr_spam_imap_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    strength int4 NOT NULL,
    msg_size_limit int4 NOT NULL,
    msg_action char(1) NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

CREATE TABLE settings.tr_spam_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    smtp_inbound int8 NOT NULL,
    smtp_outbound int8 NOT NULL,
    pop_inbound int8 NOT NULL,
    pop_outbound int8 NOT NULL,
    imap_inbound int8 NOT NULL,
    imap_outbound int8 NOT NULL,
    PRIMARY KEY (settings_id));

-- BEGIN dirty hack, make settings.tr_clamphish_settings
CREATE TABLE settings.tr_clamphish_settings (
    spam_settings_id int8 NOT NULL,
    enable_google_sb bool NOT NULL,
    PRIMARY KEY (spam_settings_id));

ALTER TABLE settings.tr_clamphish_settings
    ADD CONSTRAINT fk_clamphish_to_spam_settings
    FOREIGN KEY (spam_settings_id)
    REFERENCES settings.tr_spam_settings;
-- END dirty hack, make settings.tr_clamphish_settings

-- com.untangle.tran.spam.SpamSettings.spamRBLList (list construct)
CREATE TABLE settings.tr_spam_rbl_list (
    settings_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

-- com.untangle.tran.spam.SpamRBL
CREATE TABLE settings.tr_spam_rbl (
    id int8 NOT NULL,
    hostname text NOT NULL,
    active bool NOT NULL,
    description text NULL,
    PRIMARY KEY (id));

-- com.untangle.tran.spam.SpamSettings.spamAssassinDefList (list construct)
CREATE TABLE settings.tr_spam_spamassassin_def_list (
    settings_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

-- com.untangle.tran.spam.SpamAssassinDef
CREATE TABLE settings.tr_spam_spamassassin_def (
    id int8 NOT NULL,
    optname text NOT NULL,
    optvalue text NULL,
    active bool NOT NULL,
    description text NULL,
    PRIMARY KEY (id));

-- com.untangle.tran.spam.SpamSettings.spamAssassinLclList (list construct)
CREATE TABLE settings.tr_spam_spamassassin_lcl_list (
    settings_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

-- com.untangle.tran.spam.SpamAssassinLcl
CREATE TABLE settings.tr_spam_spamassassin_lcl (
    id int8 NOT NULL,
    optname text NOT NULL,
    optvalue text NULL,
    active bool NOT NULL,
    description text NULL,
    PRIMARY KEY (id));

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.tr_spam_settings
    ADD CONSTRAINT fk_settings_to_tid
    FOREIGN KEY (tid)
    REFERENCES settings.tid;

ALTER TABLE settings.tr_spam_settings
    ADD CONSTRAINT fk_in_ss_smtp_cfg
    FOREIGN KEY (smtp_inbound)
    REFERENCES settings.tr_spam_smtp_config;

ALTER TABLE settings.tr_spam_settings
    ADD CONSTRAINT fk_out_ss_smtp_cfg
    FOREIGN KEY (smtp_outbound)
    REFERENCES settings.tr_spam_smtp_config;

ALTER TABLE settings.tr_spam_settings
    ADD CONSTRAINT fk_in_ss_pop_cfg
    FOREIGN KEY (pop_inbound)
    REFERENCES settings.tr_spam_pop_config;

ALTER TABLE settings.tr_spam_settings
    ADD CONSTRAINT fk_out_ss_pop_cfg
    FOREIGN KEY (pop_outbound)
    REFERENCES settings.tr_spam_pop_config;

ALTER TABLE settings.tr_spam_settings
    ADD CONSTRAINT fk_in_ss_imap_cfg
    FOREIGN KEY (imap_inbound)
    REFERENCES settings.tr_spam_imap_config;

ALTER TABLE settings.tr_spam_settings
    ADD CONSTRAINT fk_out_ss_imap_cfg
    FOREIGN KEY (imap_outbound)
    REFERENCES settings.tr_spam_imap_config;
