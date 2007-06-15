-- events conversion for release 4.2
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

CREATE TABLE events.new_tr_mail_message_info_addr (
    id int8 NOT NULL,
    addr text NOT NULL,
    personal text,
    kind char(1),
    msg_id int8,
    position int4,
    time_stamp timestamp,
    PRIMARY KEY (id));

INSERT INTO events.new_tr_mail_message_info_addr
  SELECT addr.*, msg.time_stamp FROM tr_mail_message_info_addr addr
  INNER JOIN tr_mail_message_info msg ON (msg.id = addr.msg_id);

DROP TABLE events.tr_mail_message_info_addr;
ALTER TABLE events.new_tr_mail_message_info_addr RENAME TO tr_mail_message_info_addr;

CREATE TABLE events.new_tr_mail_message_stats (
    id int8 NOT NULL,
    msg_id int8,
    msg_bytes int8,
    msg_attachments int4,
    time_stamp timestamp,
    PRIMARY KEY (id));

INSERT INTO events.new_tr_mail_message_stats
  SELECT stats.*, msg.time_stamp FROM tr_mail_message_stats stats
  INNER JOIN tr_mail_message_info msg ON (msg.id = stats.msg_id);

DROP TABLE events.tr_mail_message_stats;
ALTER TABLE events.new_tr_mail_message_stats RENAME TO tr_mail_message_stats;


DROP INDEX events.tr_mail_mio_plepid_idx;
CREATE INDEX tr_mail_message_info_plepid_idx ON events.tr_mail_message_info (pl_endp_id);

CREATE INDEX tr_mail_message_info_addr_parent_idx ON events.tr_mail_message_info_addr (msg_id);
