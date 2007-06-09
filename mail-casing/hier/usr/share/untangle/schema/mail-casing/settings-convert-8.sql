-- settings conversion for release-5.0

ALTER TABLE settings.tr_mail_settings RENAME TO n_mail_settings;
ALTER TABLE settings.tr_mail_quarantine_settings RENAME TO n_mail_quarantine_settings;
ALTER TABLE settings.tr_mail_safels_recipient RENAME TO n_mail_safels_recipient;
ALTER TABLE settings.tr_mail_safels_sender RENAME TO n_mail_safels_sender;
ALTER TABLE settings.tr_mail_safels_settings RENAME TO n_mail_safels_settings;
ALTER TABLE settings.tr_mail_safelists RENAME TO n_mail_safelists;
ALTER TABLE settings.email_addr_pair_rule RENAME TO n_mail_email_addr_pair_rule;
ALTER TABLE settings.email_addr_rule RENAME TO n_mail_email_addr_rule;
