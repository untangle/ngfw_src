-- settings convert for release 4.0
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

-- add throttle flag , false for everything except smtp inbound on spamassassin
ALTER TABLE tr_spam_smtp_config ADD COLUMN throttle BOOL;
UPDATE tr_spam_smtp_config SET throttle = false;
UPDATE tr_spam_smtp_config SET throttle = true FROM tr_spam_settings, transform_persistent_state WHERE config_id = smtp_inbound and tr_spam_settings.tid = transform_persistent_state.tid and name = 'spamassassin-transform';
ALTER TABLE tr_spam_smtp_config ALTER COLUMN throttle SET NOT NULL;

-- add second throttle
ALTER TABLE tr_spam_smtp_config ADD COLUMN throttle_sec INT4;
UPDATE tr_spam_smtp_config SET throttle_sec = 15;
ALTER TABLE tr_spam_smtp_config ALTER COLUMN throttle_sec SET NOT NULL;


