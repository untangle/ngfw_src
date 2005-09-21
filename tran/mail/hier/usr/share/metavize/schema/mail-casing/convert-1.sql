-- converter to version 3.0

CREATE INDEX tr_mail_mioa_parent_idx ON events.tr_mail_message_info_addr (msg_id);

ALTER TABLE settings.tr_mail_settings
    DROP CONSTRAINT fk_tr_mail_settings_tid;

ALTER TABLE settings.tr_mail_settings
    DROP COLUMN tid;
