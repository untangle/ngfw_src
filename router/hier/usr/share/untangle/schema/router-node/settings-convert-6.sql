-- settings convert for release 3.1

ALTER TABLE tr_nat_settings DROP CONSTRAINT tr_nat_settings_uk;
ALTER TABLE tr_nat_settings ADD CONSTRAINT tr_nat_settings_tid_key UNIQUE (tid);

