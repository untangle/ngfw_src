-- settings convert for release 3.1

ALTER TABLE tr_protofilter_settings DROP CONSTRAINT tr_protofilter_settings_uk;
ALTER TABLE tr_protofilter_settings ADD CONSTRAINT tr_protofilter_settings_tid_key UNIQUE (tid);
