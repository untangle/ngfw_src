-- settings conversion for release-5.0

ALTER TABLE settings.tr_httpblk_template RENAME TO n_webfilter_template;
ALTER TABLE settings.tr_httpblk_passed_urls RENAME TO n_webfilter_passed_urls;
ALTER TABLE settings.tr_httpblk_settings RENAME TO n_webfilter_settings;
ALTER TABLE settings.tr_httpblk_extensions RENAME TO n_webfilter_extensions;
ALTER TABLE settings.tr_httpblk_mime_types RENAME TO n_webfilter_mime_types;
ALTER TABLE settings.tr_httpblk_passed_clients RENAME TO n_webfilter_passed_clients;
ALTER TABLE settings.tr_httpblk_blocked_urls RENAME TO n_webfilter_blocked_urls;
ALTER TABLE settings.tr_httpblk_blcat RENAME TO n_webfilter_blcat;
