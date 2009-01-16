-- settings schema for release-6.1
-- $HeadURL$
-- Copyright (c) 2003-2007 Untangle, Inc.
--
-- This program is free software; you can redistribute it and/or modify
-- it under the terms of the GNU General Public License, version 2,
-- as published by the Free Software Foundation.
--
-- This program is distributed in the hope that it will be useful, but
-- AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
-- NONINFRINGEMENT.  See the GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with this program; if not, write to the Free Software
-- Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
--

-------------
-- settings |
-------------

CREATE TABLE settings.n_spam_smtp_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    strength int4 NOT NULL,
    add_spam_headers bool NOT NULL,
    block_superspam bool NOT NULL,
    superspam_strength int4 NOT NULL,
    fail_closed bool NOT NULL,
    msg_size_limit int4 NOT NULL,
    msg_action char(1) NOT NULL,
    notify_action char(1) NOT NULL,
    notes varchar(255),
    throttle boolean NOT NULL,
    throttle_sec int4 NOT NULL,
    PRIMARY KEY (config_id));

CREATE TABLE settings.n_spam_pop_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    strength int4 NOT NULL,
    add_spam_headers bool NOT NULL,
    msg_size_limit int4 NOT NULL,
    msg_action char(1) NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

CREATE TABLE settings.n_spam_imap_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    strength int4 NOT NULL,
    add_spam_headers bool NOT NULL,
    msg_size_limit int4 NOT NULL,
    msg_action char(1) NOT NULL,
    notes varchar(255),
    PRIMARY KEY (config_id));

CREATE TABLE settings.n_spam_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    smtp_config int8 NOT NULL,
    pop_config int8 NOT NULL,
    imap_config int8 NOT NULL,
    PRIMARY KEY (settings_id));

-- BEGIN dirty hack, make settings.n_phish_settings
CREATE TABLE settings.n_phish_settings (
    spam_settings_id int8 NOT NULL,
    enable_google_sb bool NOT NULL,
    PRIMARY KEY (spam_settings_id));

ALTER TABLE settings.n_phish_settings
    ADD CONSTRAINT fk_clamphish_to_spam_settings
    FOREIGN KEY (spam_settings_id)
    REFERENCES settings.n_spam_settings;
-- END dirty hack, make settings.n_phish_settings

-- com.untangle.tran.spam.SpamSettings.spamRBLList (list construct)
CREATE TABLE settings.n_spam_rbl_list (
    settings_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

-- com.untangle.tran.spam.SpamRBL
CREATE TABLE settings.n_spam_rbl (
    id int8 NOT NULL,
    hostname text NOT NULL,
    active bool NOT NULL,
    description text NULL,
    PRIMARY KEY (id));

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.n_spam_settings
    ADD CONSTRAINT fk_settings_to_tid
    FOREIGN KEY (tid)
    REFERENCES settings.u_tid;

ALTER TABLE settings.n_spam_settings
    ADD CONSTRAINT fk_in_ss_smtp_cfg
    FOREIGN KEY (smtp_config)
    REFERENCES settings.n_spam_smtp_config;

ALTER TABLE settings.n_spam_settings
    ADD CONSTRAINT fk_out_ss_pop_cfg
    FOREIGN KEY (pop_config)
    REFERENCES settings.n_spam_pop_config;

ALTER TABLE settings.n_spam_settings
    ADD CONSTRAINT fk_out_ss_imap_cfg
    FOREIGN KEY (imap_config)
    REFERENCES settings.n_spam_imap_config;
