-- reports start for release-4.1

DROP TABLE reports.emails;
DROP TABLE reports.emails_smtp;

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

INSERT INTO reports.emails
  SELECT msg.id as msg_id, subject, server_type, time_stamp, recip_addr.addr AS recip_addr, recip_addr.kind AS recip_kind, from_addr.addr AS from_addr, c_client_addr, s_server_addr
    FROM tr_mail_message_info msg
         LEFT OUTER JOIN tr_mail_message_info_addr recip_addr ON (msg.id = recip_addr.msg_id AND recip_addr.kind = 'U')
         LEFT OUTER JOIN tr_mail_message_info_addr from_addr ON (msg.id = from_addr.msg_id AND from_addr.kind = 'F')
         JOIN pl_endp endp ON (endp.event_id = msg.pl_endp_id);

INSERT INTO reports.emails_smtp
  SELECT msg.id as msg_id, subject, server_type, time_stamp, recip_addr.addr AS recip_addr, recip_addr.kind AS recip_kind, from_addr.addr AS from_addr, c_client_addr, s_server_addr
    FROM tr_mail_message_info msg
         LEFT OUTER JOIN tr_mail_message_info_addr recip_addr ON (msg.id = recip_addr.msg_id AND recip_addr.kind = 'B')
         LEFT OUTER JOIN tr_mail_message_info_addr from_addr ON (msg.id = from_addr.msg_id AND from_addr.kind = 'F')
         JOIN pl_endp endp ON (endp.event_id = msg.pl_endp_id);

CREATE INDEX emails_ts_idx on reports.emails (time_stamp);
