-- schema for release-3.3

-- New column for the public port to use.
-- Set to default of 1194
alter table settings.tr_openvpn_settings add column public_port int4;
update settings.tr_openvpn_settings set public_port=1194;

