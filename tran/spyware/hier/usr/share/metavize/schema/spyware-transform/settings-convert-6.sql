-- convert script for release 3.2

-- force spyware lists to reinitialize
UPDATE tr_spyware_settings set cookie_version = -1;
UPDATE tr_spyware_settings set activex_version = -1;
UPDATE tr_spyware_settings set subnet_version = -1;
