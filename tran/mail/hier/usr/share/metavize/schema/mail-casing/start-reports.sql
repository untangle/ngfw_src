-- reports start for release-3.2

CREATE SCHEMA reports;

CREATE TABLE reports.emails AS
  SELECT msg.id as msg_id, recip_addr.addr AS recip_addr, from_addr.addr AS from_addr, c_client_addr
    FROM tr_mail_message_info msg
    LEFT OUTER JOIN tr_mail_message_info_addr recip_addr ON (msg.id = recip_addr.msg_id AND recip_addr.kind = 'B')
    LEFT OUTER JOIN tr_mail_message_info_addr from_addr ON (msg.id = from_addr.msg_id AND from_addr.kind = 'F')
    JOIN pl_endp endp ON endp.event_id = msg.pl_endp_id;

CREATE INDEX emails_mid_idx ON reports.emails (msg_id);
