-- settings schema for release-5.0
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

CREATE TABLE settings.n_virus_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    disable_ftp_resume bool NOT NULL,
    disable_http_resume bool NOT NULL,
    trickle_percent int4,
    http_config int8 NOT NULL,
    ftp_config int8 NOT NULL,
    smtp_config int8 NOT NULL,
    pop_config int8 NOT NULL,
    imap_config int8 NOT NULL,
    ftp_disable_resume_details text,
    http_disable_resume_details text,
    trickle_percent_details text,
    PRIMARY KEY (settings_id));

CREATE TABLE settings.n_virus_vs_ext (
    settings_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

CREATE TABLE settings.n_virus_config (
    config_id int8 NOT NULL,
    scan bool,
    copy_on_block bool,
    notes text,
    copy_on_block_notes text,
    PRIMARY KEY (config_id));

CREATE TABLE settings.n_virus_smtp_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    action char(1) NOT NULL,
    notify_action char(1) NOT NULL,
    notes text,
    PRIMARY KEY (config_id));

CREATE TABLE settings.n_virus_pop_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    action char(1) NOT NULL,
    notes text,
    PRIMARY KEY (config_id));

CREATE TABLE settings.n_virus_imap_config (
    config_id int8 NOT NULL,
    scan bool NOT NULL,
    action char(1) NOT NULL,
    notes text,
    PRIMARY KEY (config_id));

CREATE TABLE settings.n_virus_vs_mt (
    settings_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.n_virus_vs_ext
    ADD CONSTRAINT fk_tr_virus_vs_ext
    FOREIGN KEY (settings_id)
    REFERENCES settings.n_virus_settings;

ALTER TABLE settings.n_virus_vs_mt
    ADD CONSTRAINT fk_tr_virus_vs_mt
    FOREIGN KEY (settings_id)
    REFERENCES settings.n_virus_settings;

ALTER TABLE settings.n_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings
    FOREIGN KEY (tid)
    REFERENCES settings.u_tid;

ALTER TABLE settings.n_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_ftpout
    FOREIGN KEY (ftp_config)
    REFERENCES settings.n_virus_config;

ALTER TABLE settings.n_virus_settings
    ADD CONSTRAINT fk_tr_virus_set_httpout
    FOREIGN KEY (http_config)
    REFERENCES settings.n_virus_config;

ALTER TABLE settings.n_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_smtpin
    FOREIGN KEY (smtp_config)
    REFERENCES settings.n_virus_smtp_config;

ALTER TABLE settings.n_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_popout
    FOREIGN KEY (pop_config)
    REFERENCES settings.n_virus_pop_config;

ALTER TABLE settings.n_virus_settings
    ADD CONSTRAINT fk_tr_virus_settings_imapout
    FOREIGN KEY (imap_config)
    REFERENCES settings.n_virus_imap_config;
