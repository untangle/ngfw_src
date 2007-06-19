-- reports start for release-4.1
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

--------------------------------------------------------------------------------
-- Create master tables as necessary, ignore errors if already present.
SET search_path TO reports,events,public;

CREATE TABLE reports.emails (
        msg_id int8 NOT NULL,
        time_stamp timestamp NOT NULL,
        subject text,
        server_type char(1),
        recip_addr text,
        recip_kind char(1),
        from_addr text,
        c_client_addr inet,
        s_server_addr inet,
        PRIMARY KEY (msg_id));
-- SMTP emails
CREATE TABLE reports.emails_smtp (
        msg_id int8 NOT NULL,
        time_stamp timestamp NOT NULL,
        subject text,
        server_type char(1),
        recip_addr text,
        recip_kind char(1),
        from_addr text,
        c_client_addr inet,
        s_server_addr inet,
        PRIMARY KEY (msg_id));

-- Just in case
DELETE FROM ONLY reports.emails;
DELETE FROM ONLY reports.emails_smtp;

--------------------------------------------------------------------------------
-- Do the day
DROP TABLE emails_:dayname;
CREATE TABLE emails_:dayname (
    CHECK (time_stamp >= TIMESTAMP :daybegin AND time_stamp < TIMESTAMP :dayend)
) INHERITS (emails);

DROP TABLE emails_smtp_:dayname;
CREATE TABLE emails_smtp_:dayname (
    CHECK (time_stamp >= TIMESTAMP :daybegin AND time_stamp < TIMESTAMP :dayend)
) INHERITS (emails_smtp);

DROP TABLE reports.newemails;
CREATE TABLE reports.newemails AS
  SELECT msg.id as msg_id, subject, server_type, msg.time_stamp, c_client_addr, s_server_addr
    FROM tr_mail_message_info msg
    JOIN pl_endp endp ON (endp.event_id = msg.pl_endp_id)
   WHERE msg.time_stamp >= TIMESTAMP :daybegin AND msg.time_stamp <= TIMESTAMP :dayend;

INSERT INTO reports.emails_:dayname
  SELECT msg.msg_id, msg.time_stamp, subject, server_type, recip_addr.addr AS recip_addr, recip_addr.kind AS recip_kind, from_addr.addr AS from_addr, c_client_addr, s_server_addr
    FROM reports.newemails msg
         LEFT OUTER JOIN tr_mail_message_info_addr recip_addr ON (msg.msg_id = recip_addr.msg_id AND recip_addr.kind = 'U' and recip_addr.position = 1)
         LEFT OUTER JOIN tr_mail_message_info_addr from_addr ON (msg.msg_id = from_addr.msg_id AND from_addr.kind = 'F' and from_addr.position = 1)
   WHERE server_type != 'S';

INSERT INTO reports.emails_smtp_:dayname
  SELECT msg.msg_id, msg.time_stamp, subject, server_type, recip_addr.addr AS recip_addr, recip_addr.kind AS recip_kind, from_addr.addr AS from_addr, c_client_addr, s_server_addr
    FROM reports.newemails msg
         LEFT OUTER JOIN tr_mail_message_info_addr recip_addr ON (msg.msg_id = recip_addr.msg_id AND recip_addr.kind = 'B' and recip_addr.position = 1)
         LEFT OUTER JOIN tr_mail_message_info_addr from_addr ON (msg.msg_id = from_addr.msg_id AND from_addr.kind = 'F' and from_addr.position = 1)
   WHERE server_type = 'S';

DROP TABLE newemails;

CREATE INDEX emails_msgid_idx_:dayname ON emails_:dayname (msg_id);
CREATE INDEX emails_ts_idx_:dayname ON emails_:dayname (time_stamp);

CREATE INDEX emails_smtp_msgid_idx_:dayname ON emails_smtp_:dayname (msg_id);
CREATE INDEX emails_smtp_ts_idx_:dayname ON emails_smtp_:dayname (time_stamp);
