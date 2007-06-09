-- settings conversion for release-5.0

ALTER TABLE settings.tr_virus_settings RENAME TO n_virus_settings;
ALTER TABLE settings.tr_virus_vs_ext RENAME TO n_virus_vs_ext;
ALTER TABLE settings.tr_virus_config RENAME TO n_virus_config;
ALTER TABLE settings.tr_virus_smtp_config RENAME TO n_virus_smtp_config;
ALTER TABLE settings.tr_virus_pop_config RENAME TO n_virus_pop_config;
ALTER TABLE settings.tr_virus_imap_config RENAME TO n_virus_imap_config;
ALTER TABLE settings.tr_virus_vs_mt RENAME TO n_virus_vs_mt;
