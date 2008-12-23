-- settings conversion for release-6.1
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

ALTER TABLE settings.n_spam_smtp_config ADD COLUMN block_superspam bool;
UPDATE settings.n_spam_smtp_config SET block_superspam = false;
ALTER TABLE settings.n_spam_smtp_config ALTER COLUMN block_superspam SET NOT NULL;

ALTER TABLE settings.n_spam_smtp_config ADD COLUMN superspam_strength int4;
UPDATE settings.n_spam_smtp_config SET superspam_strength = 90;
ALTER TABLE settings.n_spam_smtp_config ALTER COLUMN block_superspam SET NOT NULL;

ALTER TABLE settings.n_spam_pop_config ADD COLUMN block_superspam bool;
UPDATE settings.n_spam_pop_config SET block_superspam = false;
ALTER TABLE settings.n_spam_pop_config ALTER COLUMN block_superspam SET NOT NULL;

ALTER TABLE settings.n_spam_pop_config ADD COLUMN superspam_strength int4;
UPDATE settings.n_spam_pop_config SET superspam_strength = 90;
ALTER TABLE settings.n_spam_pop_config ALTER COLUMN block_superspam SET NOT NULL;

ALTER TABLE settings.n_spam_imap_config ADD COLUMN block_superspam bool;
UPDATE settings.n_spam_imap_config SET block_superspam = false;
ALTER TABLE settings.n_spam_imap_config ALTER COLUMN block_superspam SET NOT NULL;

ALTER TABLE settings.n_spam_imap_config ADD COLUMN superspam_strength int4;
UPDATE settings.n_spam_imap_config SET superspam_strength = 90;
ALTER TABLE settings.n_spam_imap_config ALTER COLUMN block_superspam SET NOT NULL;
