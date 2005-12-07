-- settings conversion for release-3.1

ALTER TABLE tr_reporting_settings DROP CONSTRAINT tr_reporting_settings_uk;
ALTER TABLE tr_reporting_settings ADD CONSTRAINT tr_reporting_settings_tid_key UNIQUE (tid);
