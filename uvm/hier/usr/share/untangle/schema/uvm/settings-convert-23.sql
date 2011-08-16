-- settings conversion for release-9.1

ALTER TABLE settings.u_ab_settings
  DROP COLUMN ab_configuration;
