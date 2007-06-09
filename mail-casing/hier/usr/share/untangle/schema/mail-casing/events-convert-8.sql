-- events conversion for release-5.0

ALTER TABLE events.tr_mail_message_info RENAME TO n_mail_message_info;
ALTER TABLE events.tr_mail_message_info_addr RENAME TO n_mail_message_info_addr;
ALTER TABLE events.tr_mail_message_stats RENAME TO n_mail_message_stats;
