-- reports start for release-3.2

CREATE SCHEMA reports;

DROP TABLE reports.emails;
DROP TABLE reports.emails_smtp;

-- POP/IMAP emails
CREATE TABLE reports.emails AS
  SELECT msg.id as msg_id, subject, server_type, recip_addr.addr AS recip_addr, recip_addr.kind AS recip_kind, from_addr.addr AS from_addr, c_client_addr, s_server_addr
    FROM tr_mail_message_info msg
         LEFT OUTER JOIN tr_mail_message_info_addr recip_addr ON (msg.id = recip_addr.msg_id AND recip_addr.kind = 'U')
         LEFT OUTER JOIN tr_mail_message_info_addr from_addr ON (msg.id = from_addr.msg_id AND from_addr.kind = 'F')
         JOIN pl_endp endp ON (endp.event_id = msg.pl_endp_id);

CREATE INDEX emails_mid_idx ON reports.emails (msg_id);

-- SMTP emails
CREATE TABLE reports.emails_smtp AS
  SELECT msg.id as msg_id, subject, server_type, recip_addr.addr AS recip_addr, recip_addr.kind AS recip_kind, from_addr.addr AS from_addr, c_client_addr, s_server_addr
    FROM tr_mail_message_info msg
         LEFT OUTER JOIN tr_mail_message_info_addr recip_addr ON (msg.id = recip_addr.msg_id AND recip_addr.kind = 'B')
         LEFT OUTER JOIN tr_mail_message_info_addr from_addr ON (msg.id = from_addr.msg_id AND from_addr.kind = 'F')
         JOIN pl_endp endp ON (endp.event_id = msg.pl_endp_id);

CREATE INDEX emails_smtp_mid_idx ON reports.emails_smtp (msg_id);
