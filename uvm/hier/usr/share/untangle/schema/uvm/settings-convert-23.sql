-- settings conversion for release-9.1

ALTER TABLE settings.u_ab_settings
  DROP COLUMN ab_configuration;

ALTER TABLE settings.u_ab_repository_settings
  ADD COLUMN enabled bool;
