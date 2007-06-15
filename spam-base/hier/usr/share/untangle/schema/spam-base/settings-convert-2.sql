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

-- "notify both" action is no longer supported;
-- convert to "notify sender" action (for both spam and clamphish)
UPDATE settings.tr_spam_smtp_config
  SET notify_action = 'S' where notify_action = 'B';

-- "notify recipient" action is no longer supported;
-- convert to "notify neither" action (for both spam and clamphish)
UPDATE settings.tr_spam_smtp_config
  SET notify_action = 'N' where notify_action = 'R';

-- add NOT NULL
ALTER TABLE tr_spam_smtp_config ALTER COLUMN msg_size_limit SET NOT NULL;
ALTER TABLE tr_spam_smtp_config ALTER COLUMN strength SET NOT NULL;
ALTER TABLE tr_spam_imap_config ALTER COLUMN msg_size_limit SET NOT NULL;
ALTER TABLE tr_spam_imap_config ALTER COLUMN strength SET NOT NULL;
ALTER TABLE tr_spam_pop_config ALTER COLUMN msg_size_limit SET NOT NULL;
ALTER TABLE tr_spam_pop_config ALTER COLUMN strength SET NOT NULL;
