-- settings convert for release 3.1
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

--------------------
-- remove varchars |
--------------------

-- settings.tr_virus_settings

ALTER TABLE settings.tr_virus_settings ADD COLUMN tmp text;
UPDATE settings.tr_virus_settings SET tmp = ftp_disable_resume_details;
ALTER TABLE settings.tr_virus_settings DROP COLUMN ftp_disable_resume_details;
ALTER TABLE settings.tr_virus_settings RENAME COLUMN tmp TO ftp_disable_resume_details;

ALTER TABLE settings.tr_virus_settings ADD COLUMN tmp text;
UPDATE settings.tr_virus_settings SET tmp = http_disable_resume_details;
ALTER TABLE settings.tr_virus_settings DROP COLUMN http_disable_resume_details;
ALTER TABLE settings.tr_virus_settings RENAME COLUMN tmp TO http_disable_resume_details;

ALTER TABLE settings.tr_virus_settings ADD COLUMN tmp text;
UPDATE settings.tr_virus_settings SET tmp = trickle_percent_details;
ALTER TABLE settings.tr_virus_settings DROP COLUMN trickle_percent_details;
ALTER TABLE settings.tr_virus_settings RENAME COLUMN tmp TO trickle_percent_details;

-- settings.tr_virus_config

ALTER TABLE settings.tr_virus_config ADD COLUMN tmp text;
UPDATE settings.tr_virus_config SET tmp = notes;
ALTER TABLE settings.tr_virus_config DROP COLUMN notes;
ALTER TABLE settings.tr_virus_config RENAME COLUMN tmp TO notes;

ALTER TABLE settings.tr_virus_config ADD COLUMN tmp text;
UPDATE settings.tr_virus_config SET tmp = copy_on_block_notes;
ALTER TABLE settings.tr_virus_config DROP COLUMN copy_on_block_notes;
ALTER TABLE settings.tr_virus_config RENAME COLUMN tmp TO copy_on_block_notes;

-- settings.tr_virus_smtp_config

ALTER TABLE settings.tr_virus_smtp_config ADD COLUMN tmp text;
UPDATE settings.tr_virus_smtp_config SET tmp = notes;
ALTER TABLE settings.tr_virus_smtp_config DROP COLUMN notes;
ALTER TABLE settings.tr_virus_smtp_config RENAME COLUMN tmp TO notes;


-- settings.tr_virus_pop_config

ALTER TABLE settings.tr_virus_pop_config ADD COLUMN tmp text;
UPDATE settings.tr_virus_pop_config SET tmp = notes;
ALTER TABLE settings.tr_virus_pop_config DROP COLUMN notes;
ALTER TABLE settings.tr_virus_pop_config RENAME COLUMN tmp TO notes;

-- settings.tr_virus_imap_config

ALTER TABLE settings.tr_virus_imap_config ADD COLUMN tmp text;
UPDATE settings.tr_virus_imap_config SET tmp = notes;
ALTER TABLE settings.tr_virus_imap_config DROP COLUMN notes;
ALTER TABLE settings.tr_virus_imap_config RENAME COLUMN tmp TO notes;

-- rename constraints

ALTER TABLE tr_virus_settings DROP CONSTRAINT tr_virus_settings_uk;
ALTER TABLE tr_virus_settings ADD CONSTRAINT tr_virus_settings_tid_key UNIQUE (tid);
