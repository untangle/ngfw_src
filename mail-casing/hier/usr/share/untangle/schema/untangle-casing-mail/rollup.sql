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

--due to hibernate mapping/cascade issue,
--we must manually delete orphaned child data

--delete orphaned recipients
DELETE FROM settings.tr_mail_safels_recipient
  WHERE id NOT IN
    (SELECT DISTINCT recipient FROM settings.tr_mail_safels_settings);

--delete orphaned senders
DELETE FROM settings.tr_mail_safels_sender
  WHERE id NOT IN
    (SELECT DISTINCT sender FROM settings.tr_mail_safels_settings);
