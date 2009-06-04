-- settings conversion for release-6.3
-- $HeadURL: svn://chef/work/src/mail-casing/hier/usr/share/untangle/schema/untangle-casing-mail/settings-convert-8.sql $
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

ALTER TABLE settings.n_mail_quarantine_settings DROP COLUMN quarantine_external_mail;

ALTER TABLE settings.n_mail_quarantine_settings ADD COLUMN send_daily_digests bool;
UPDATE settings.n_mail_quarantine_settings SET send_daily_digests = true;
ALTER TABLE settings.n_mail_quarantine_settings ALTER COLUMN send_daily_digests SET NOT NULL;

