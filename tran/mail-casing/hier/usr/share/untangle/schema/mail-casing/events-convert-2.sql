-- events converter for release 3.1

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
