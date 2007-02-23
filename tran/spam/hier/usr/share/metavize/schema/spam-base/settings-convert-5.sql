-- settings convert for release 4.2

-------------
-- settings |
-------------

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
