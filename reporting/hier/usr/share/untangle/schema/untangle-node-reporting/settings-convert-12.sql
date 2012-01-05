ALTER TABLE settings.n_reporting_settings ADD COLUMN nightly_hour INT4;
UPDATE settings.n_reporting_settings SET nightly_hour = 2;
ALTER TABLE settings.n_reporting_settings ALTER COLUMN nightly_hour SET NOT NULL;

ALTER TABLE settings.n_reporting_settings ADD COLUMN nightly_minute INT4;
UPDATE settings.n_reporting_settings SET nightly_minute = 0;
ALTER TABLE settings.n_reporting_settings ALTER COLUMN nightly_minute SET NOT NULL;

