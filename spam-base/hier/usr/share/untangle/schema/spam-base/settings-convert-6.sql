-- settings conversion for release-5.0

ALTER TABLE settings.tr_spam_smtp_config RENAME TO n_spam_smtp_config;
ALTER TABLE settings.tr_spam_pop_config RENAME TO n_spam_pop_config;
ALTER TABLE settings.tr_spam_imap_config RENAME TO n_spam_imap_config;
ALTER TABLE settings.tr_spam_settings RENAME TO n_spam_settings;
ALTER TABLE settings.tr_clamphish_settings RENAME TO n_phish_settings;
ALTER TABLE settings.tr_spam_rbl_list RENAME TO n_spam_rbl_list;
ALTER TABLE settings.tr_spam_rbl RENAME TO n_spam_rbl;
ALTER TABLE settings.tr_spam_spamassassin_def_list RENAME TO n_spamassassin_def_list;
ALTER TABLE settings.tr_spam_spamassassin_def RENAME TO n_spamassassin_def;
ALTER TABLE settings.tr_spam_spamassassin_lcl_list RENAME TO n_spamassassin_lcl_list;
ALTER TABLE settings.tr_spam_spamassassin_lcl RENAME TO n_spamassassin_lcl;
