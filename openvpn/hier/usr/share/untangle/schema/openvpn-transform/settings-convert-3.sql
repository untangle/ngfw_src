-- schema for release-4.1

-- New column for settings to override DNS settings
ALTER TABLE settings.tr_openvpn_settings ADD COLUMN is_dns_override BOOL;

-- New column for settings for Primary DNS override
ALTER TABLE settings.tr_openvpn_settings ADD COLUMN dns_1 INET;

-- New column for settings for Secondary DNS override
ALTER TABLE settings.tr_openvpn_settings ADD COLUMN dns_2 INET;

-- Set to false by default
UPDATE settings.tr_openvpn_settings SET is_dns_override=false, dns_1 = NULL, dns_2 = NULL;


