-- events conversion for release-5.0
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

ALTER TABLE events.tr_spam_evt_smtp RENAME TO n_spam_evt_smtp;
ALTER TABLE events.tr_spam_evt RENAME TO n_spam_evt;
ALTER TABLE events.tr_spam_smtp_rbl_evt RENAME TO n_spam_smtp_rbl_evt;

ALTER TABLE events.tr_spam_evt_smtp_ts_idx RENAME TO n_spam_evt_smtp_ts_idx;
ALTER TABLE events.tr_spam_evt_ts_idx RENAME TO n_spam_evt_ts_idx;
ALTER TABLE events.tr_spam_evt_mid_idx RENAME TO n_spam_evt_mid_idx;
ALTER TABLE events.tr_spam_evt_smtp_mid_idx RENAME TO n_spam_evt_smtp_mid_idx;
ALTER TABLE events.tr_spam_smtp_rbl_evt_ts_idx RENAME TO n_spam_smtp_rbl_evt_ts_idx;
