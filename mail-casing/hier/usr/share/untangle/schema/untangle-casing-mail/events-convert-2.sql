-- events converter for release 3.1
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

---------------------
-- point to pl_endp |
---------------------

DROP TABLE events.tr_mail_tmp;

CREATE TABLE events.tr_mail_tmp AS
    SELECT id, event_id AS pl_endp_id, subject::text, server_type
    FROM events.tr_mail_message_info JOIN events.pl_endp USING (session_id);

DROP TABLE events.tr_mail_message_info;
ALTER TABLE events.tr_mail_tmp RENAME TO tr_mail_message_info;
ALTER TABLE events.tr_mail_message_info ALTER COLUMN id SET NOT NULL;
ALTER TABLE events.tr_mail_message_info ALTER COLUMN subject SET NOT NULL;
ALTER TABLE events.tr_mail_message_info ALTER COLUMN server_type SET NOT NULL;
ALTER TABLE events.tr_mail_message_info ADD PRIMARY KEY (id);

-------------------
-- remove varchar |
-------------------

-- events.tr_mail_message_info_addr

DROP TABLE events.tr_mail_tmp;

CREATE TABLE events.tr_mail_tmp AS
    SELECT addr.id, addr::text, personal::text, kind, msg_id, position
    FROM events.tr_mail_message_info_addr addr
         JOIN events.tr_mail_message_info info ON addr.msg_id = info.id;

DROP TABLE events.tr_mail_message_info_addr;
ALTER TABLE events.tr_mail_tmp RENAME TO tr_mail_message_info_addr;
ALTER TABLE events.tr_mail_message_info_addr ALTER COLUMN id SET NOT NULL;
ALTER TABLE events.tr_mail_message_info_addr ALTER COLUMN addr SET NOT NULL;
ALTER TABLE events.tr_mail_message_info_addr ADD PRIMARY KEY (id);

----------------
-- constraints |
----------------

-- foreign key constraints
ALTER TABLE events.tr_mail_message_info_addr
    ADD CONSTRAINT fk_trml_msginfoaddr_to_msginfo
    FOREIGN KEY (msg_id)
    REFERENCES tr_mail_message_info;
