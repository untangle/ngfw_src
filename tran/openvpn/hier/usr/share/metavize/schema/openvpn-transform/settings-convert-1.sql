-- schema for release-3.2

-- New column by-group (pool) for enabling client-side DNS
-- Set to false by default
alter table settings.tr_openvpn_group add column use_dns bool;
update settings.tr_openvpn_group set use_dns=false;
