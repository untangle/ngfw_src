-- settings conversion for release-5.0

ALTER TABLE settings.tr_protofilter_settings RENAME TO n_protofilter_settings;
ALTER TABLE settings.tr_protofilter_pattern RENAME TO n_protofilter_pattern;
