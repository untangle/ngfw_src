-- settings schema for release-6.0
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

-----------
-- tables |
-----------

-- com.untangle.tran.mail.papi.MailTransformSettings
CREATE TABLE settings.n_mail_settings (
    settings_id int8 NOT NULL,
    smtp_enabled bool NOT NULL,
    pop_enabled bool NOT NULL,
    imap_enabled bool NOT NULL,
    smtp_timeout int8 NOT NULL,
    pop_timeout int8 NOT NULL,
    imap_timeout int8 NOT NULL,
    quarantine_settings int8,
    smtp_allow_tls bool NOT NULL,
    PRIMARY KEY (settings_id));

-- com.untangle.tran.mail.papi.quarantine.QuarantineSettings
CREATE TABLE settings.n_mail_quarantine_settings (
    settings_id int8 NOT NULL,
    max_intern_time int8 NOT NULL,
    max_idle_inbox_time int8 NOT NULL,
    secret_key bytea NOT NULL,
    hour_in_day int4,
    minute_in_day int4,
    max_quarantine_sz int8 NOT NULL,
    quarantine_external_mail bool NOT NULL,
    PRIMARY KEY (settings_id));

-- com.untangle.tran.mail.papi.safelist.SafelistRecipient
CREATE TABLE settings.n_mail_safels_recipient (
    id int8 NOT NULL,
    addr text NOT NULL,
    PRIMARY KEY (id));

-- com.untangle.tran.mail.papi.safelist.SafelistSender
CREATE TABLE settings.n_mail_safels_sender (
    LIKE settings.n_mail_safels_recipient,
    PRIMARY KEY (id));

-- com.untangle.tran.mail.papi.safelist.SafelistSettings
CREATE TABLE settings.n_mail_safels_settings (
    safels_id int8 NOT NULL,
    recipient int8 NOT NULL,
    sender int8 NOT NULL,
    PRIMARY KEY (safels_id));

-- com.untangle.tran.mail.papi.MailTransformSettings.safelists (list construct)
CREATE TABLE settings.n_mail_safelists (
    safels_id int8 NOT NULL,
    setting_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

-- com.untangle.tran.mail.papi.EmailAddressPairRule
CREATE TABLE settings.n_mail_email_addr_pair_rule (
    rule_id int8 NOT NULL,
    address1 text,
    address2 text,
    name text,
    category text,
    description text,
    live bool,
    alert bool,
    log bool,
    position int4,
    settings_id int8,
    PRIMARY KEY (rule_id));

-- com.untangle.mvvm.tran.EmailAddressRule
CREATE TABLE settings.n_mail_email_addr_rule (
    rule_id int8 NOT NULL,
    address text,
    name text,
    category text,
    description text,
    live bool,
    alert bool,
    log bool,
    position int4,
    settings_id int8,
    PRIMARY KEY (rule_id));




----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.n_mail_safelists
    ADD CONSTRAINT fk_trml_safelists_to_ml_settings
    FOREIGN KEY (setting_id)
    REFERENCES settings.n_mail_settings;

ALTER TABLE settings.n_mail_safelists
    ADD CONSTRAINT fk_trml_safelists_to_sl_settings
    FOREIGN KEY (safels_id)
    REFERENCES settings.n_mail_safels_settings;

ALTER TABLE settings.n_mail_safels_settings
    ADD CONSTRAINT fk_trml_sl_settings_to_sl_recipient
    FOREIGN KEY (recipient)
    REFERENCES settings.n_mail_safels_recipient;

ALTER TABLE settings.n_mail_safels_settings
    ADD CONSTRAINT fk_trml_sl_settings_to_sl_sender
    FOREIGN KEY (sender)
    REFERENCES settings.n_mail_safels_sender;
