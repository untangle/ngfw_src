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

---------------------------------------------
-- copy time_stamp value from pl_endp       |
-- (temporary workaround for existing data) |
---------------------------------------------

DROP TABLE events.tr_mail_tmp;

CREATE TABLE events.tr_mail_tmp AS
  SELECT msg.id, msg.pl_endp_id, msg.subject::text, msg.server_type,
         endp.time_stamp AS time_stamp
  FROM events.tr_mail_message_info msg
    JOIN events.pl_endp endp
      ON (msg.pl_endp_id = endp.event_id);

DROP INDEX tr_mail_mio_sid_idx;
ALTER TABLE events.tr_mail_message_info_addr
  DROP CONSTRAINT fk_trml_msginfoaddr_to_msginfo;
DROP TABLE events.tr_mail_message_info;

ALTER TABLE events.tr_mail_tmp RENAME TO tr_mail_message_info;
ALTER TABLE events.tr_mail_message_info ALTER COLUMN id SET NOT NULL;
ALTER TABLE events.tr_mail_message_info ALTER COLUMN subject SET NOT NULL;
ALTER TABLE events.tr_mail_message_info ALTER COLUMN server_type SET NOT NULL;
ALTER TABLE events.tr_mail_message_info ADD PRIMARY KEY (id);

ALTER TABLE events.tr_mail_message_info_addr
  ADD CONSTRAINT fk_trml_msginfoaddr_to_msginfo
  FOREIGN KEY (msg_id)
  REFERENCES tr_mail_message_info;

DROP INDEX tr_mail_mio_sid_idx;
CREATE INDEX tr_mail_mio_plepid_idx ON events.tr_mail_message_info (pl_endp_id);

-- bye bye picasso
DROP TABLE tr_email_ctl_definition CASCADE;
DROP TABLE tr_email_custom_evt CASCADE;
DROP TABLE tr_email_handler_info CASCADE;
DROP TABLE tr_email_message_info CASCADE;
DROP TABLE tr_email_ml_definition CASCADE;
DROP TABLE tr_email_settings CASCADE;
DROP TABLE tr_email_spam_evt CASCADE;
DROP TABLE tr_email_ssctl_definition CASCADE;
DROP TABLE tr_email_szrelay_evt CASCADE;
DROP TABLE tr_email_virus_evt CASCADE;
DROP TABLE tr_email_vsctl_definition CASCADE;
