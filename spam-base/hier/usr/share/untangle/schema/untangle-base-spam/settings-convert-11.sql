-- settings conversion for release-6.3
-- $HeadURL: svn://chef/work/src/spam-base/hier/usr/share/untangle/schema/untangle-base-spam/settings-convert-6.sql $
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

-- The goal is to remap the DNSBL entries.  One way would be to just delete all.
-- but this depends on some function named initSpamRBLList never changing.
-- This technique just deletes everything except for the new rule.
-- Create a new RBL list.

ALTER TABLE settings.n_spam_smtp_config ADD COLUMN limit_load float;
UPDATE settings.n_spam_smtp_config SET limit_load = '7.0';
ALTER TABLE settings.n_spam_smtp_config ALTER COLUMN limit_load SET NOT NULL;

ALTER TABLE settings.n_spam_smtp_config ADD COLUMN limit_scans int4;
UPDATE settings.n_spam_smtp_config SET limit_scans = '15';
ALTER TABLE settings.n_spam_smtp_config ALTER COLUMN limit_scans SET NOT NULL;

ALTER TABLE settings.n_spam_smtp_config ADD COLUMN scan_wan_mail bool;
UPDATE settings.n_spam_smtp_config SET scan_wan_mail = true;
ALTER TABLE settings.n_spam_smtp_config ALTER COLUMN scan_wan_mail SET NOT NULL;

ALTER TABLE settings.n_spam_smtp_config DROP COLUMN notify_action;
ALTER TABLE settings.n_spam_smtp_config RENAME COLUMN throttle to tarpit;
ALTER TABLE settings.n_spam_smtp_config RENAME COLUMN throttle_sec to tarpit_timeout;

ALTER TABLE settings.n_spam_smtp_config DROP COLUMN notes;
ALTER TABLE settings.n_spam_imap_config DROP COLUMN notes;
ALTER TABLE settings.n_spam_pop_config DROP COLUMN notes;

