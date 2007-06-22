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

-- Create schema/tables as necessary, ignore errors if already present.

CREATE SCHEMA reports;
-- POP/IMAP emails
CREATE TABLE reports.emails (
        msg_id int8 NOT NULL,
        subject text,
        server_type char(1),
        time_stamp timestamp,
        recip_addr text,
        recip_kind char(1),
        from_addr text,
        c_client_addr inet,
        s_server_addr inet,
        PRIMARY KEY (msg_id));
-- SMTP emails
CREATE TABLE reports.emails_smtp (
        msg_id int8 NOT NULL,
        subject text,
        server_type char(1),
        time_stamp timestamp,
        recip_addr text,
        recip_kind char(1),
        from_addr text,
        c_client_addr inet,
        s_server_addr inet,
        PRIMARY KEY (msg_id));
-- CREATE INDEX emails_ts_idx on reports.emails (time_stamp);

DELETE FROM reports.emails WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM reports.emails_smtp WHERE time_stamp < (:cutoff)::timestamp;

DROP TABLE reports.newemails;
CREATE TABLE reports.newemails AS
  SELECT msg.id as msg_id, subject, server_type, msg.time_stamp, c_client_addr, s_server_addr
    FROM n_mail_message_info msg
    JOIN pl_endp endp ON (endp.event_id = msg.pl_endp_id)
   WHERE server_type != 'S'
     AND msg.time_stamp > (SELECT COALESCE(max(time_stamp), timestamp '2004-01-01') from reports.emails);

INSERT INTO reports.emails
  SELECT msg.msg_id, subject, server_type, time_stamp, recip_addr.addr AS recip_addr, recip_addr.kind AS recip_kind, from_addr.addr AS from_addr, c_client_addr, s_server_addr
    FROM reports.newemails msg
         LEFT OUTER JOIN n_mail_message_info_addr recip_addr ON (msg.msg_id = recip_addr.msg_id AND recip_addr.kind = 'U' and recip_addr.position = 1)
         LEFT OUTER JOIN n_mail_message_info_addr from_addr ON (msg.msg_id = from_addr.msg_id AND from_addr.kind = 'F' and from_addr.position = 1);

DROP TABLE reports.newemails_smtp;
CREATE TABLE reports.newemails_smtp AS
  SELECT msg.id as msg_id, subject, server_type, msg.time_stamp, c_client_addr, s_server_addr
    FROM n_mail_message_info msg
    JOIN pl_endp endp ON (endp.event_id = msg.pl_endp_id)
   WHERE server_type = 'S'
     AND msg.time_stamp > (SELECT COALESCE(max(time_stamp), timestamp '2004-01-01') from reports.emails_smtp);

INSERT INTO reports.emails_smtp
  SELECT msg.msg_id, subject, server_type, time_stamp, recip_addr.addr AS recip_addr, recip_addr.kind AS recip_kind, from_addr.addr AS from_addr, c_client_addr, s_server_addr
    FROM reports.newemails_smtp msg
         LEFT OUTER JOIN n_mail_message_info_addr recip_addr ON (msg.msg_id = recip_addr.msg_id AND recip_addr.kind = 'B' and recip_addr.position = 1)
         LEFT OUTER JOIN n_mail_message_info_addr from_addr ON (msg.msg_id = from_addr.msg_id AND from_addr.kind = 'F' and from_addr.position = 1);

DROP TABLE reports.newemails;
DROP TABLE reports.newemails_smtp;
