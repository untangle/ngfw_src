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

CREATE TABLE settings.n_phish_settings (
    spam_settings_id int8 NOT NULL,
    enable_google_sb bool NOT NULL,
    PRIMARY KEY (spam_settings_id));

-- foreign key constraints

ALTER TABLE settings.n_phish_settings
    ADD CONSTRAINT fk_clamphish_to_spam_settings
    FOREIGN KEY (spam_settings_id)
    REFERENCES settings.n_spam_settings;
