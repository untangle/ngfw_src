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

-- fix up event tables if message info data is:
-- 1- invalid (events refer to null messages)
DELETE FROM tr_spam_evt_smtp
  WHERE msg_id IS null;
DELETE FROM tr_spam_evt
  WHERE msg_id IS null;
-- 2- missing (events refer to non-existent messages)
DELETE FROM tr_spam_evt_smtp
  WHERE msg_id IN
        (SELECT msg_id
           FROM tr_spam_evt_smtp
         EXCEPT
         SELECT id
           FROM tr_mail_message_info);
DELETE FROM tr_spam_evt
  WHERE msg_id IN
        (SELECT msg_id
           FROM tr_spam_evt
         EXCEPT
         SELECT id
           FROM tr_mail_message_info);
