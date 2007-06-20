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

DROP INDEX events.tr_virus_evt_http_rid_idx;
DROP INDEX events.tr_virus_evt_http_ts_idx;
DROP INDEX events.tr_virus_evt_ts_idx;
DROP INDEX events.tr_virus_evt_smtp_ts_idx;
DROP INDEX events.tr_virus_evt_mail_ts_idx;
DROP INDEX events.tr_virus_evt_smtp_mid_idx;
DROP INDEX events.tr_virus_evt_mail_mid_idx;

CREATE INDEX n_virus_evt_http_rid_idx ON events.n_virus_evt_http (request_line);
CREATE INDEX n_virus_evt_http_ts_idx ON events.n_virus_evt_http (time_stamp);
CREATE INDEX n_virus_evt_ts_idx ON events.n_virus_evt (time_stamp);
CREATE INDEX n_virus_evt_smtp_ts_idx ON events.n_virus_evt_smtp (time_stamp);
CREATE INDEX n_virus_evt_mail_ts_idx ON events.n_virus_evt_mail (time_stamp);
CREATE INDEX n_virus_evt_smtp_mid_idx ON events.n_virus_evt_smtp (msg_id);
CREATE INDEX n_virus_evt_mail_mid_idx ON events.n_virus_evt_mail (msg_id);


ALTER TABLE events.tr_virus_evt RENAME TO n_virus_evt;
ALTER TABLE events.tr_virus_evt_http RENAME TO n_virus_evt_http;
ALTER TABLE events.tr_virus_evt_smtp RENAME TO n_virus_evt_smtp;
ALTER TABLE events.tr_virus_evt_mail RENAME TO n_virus_evt_mail;
